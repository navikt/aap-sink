package app

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.aap.avro.vedtak.v1.*
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class Test {

    @Test
    fun test() {
        withTestApp { mocks ->
            initializeTopics(mocks.kafka)
            søkere.produce("123") {
                defaultSøker()
            }

            val søker = awaitDatabase {
                Repo.search("123")
            }

            assertNotNull(søker)
            assertTrue(søker.size == 1)

            val topic = Topic(loadConfig<Config>().kafka)
            val avro = topic.avroSerde.deserializer().deserialize(topic.name, søker.single())
            assertNotNull(avro)
            assertEquals("123", avro.personident)
        }
    }

    companion object {
        internal fun initializeTopics(kafka: KStreamsMock) {
            søkere = kafka.inputAvroTopic("aap.sokere.v1")
        }

        private lateinit var søkere: TestInputTopic<String, Soker>
    }

    private fun defaultSøker(): Soker = Soker.newBuilder()
        .setPersonident("123")
        .setFodselsdato(LocalDate.now().minusYears(30))
        .setSaker(
            listOf(
                Sak.newBuilder()
                    .setTilstand("SØKNAD_MOTTATT")
                    .setVilkarsvurderinger(
                        listOf(
                            Vilkarsvurdering.newBuilder()
                                .setLedd(listOf("LEDD_1"))
                                .setParagraf("PARAGRAF FUNNY")
                                .setTilstand("SØKNAD_MOTTATT")
                                .setLosning112Manuell(Losning_11_2.newBuilder().setErMedlem("JA").build())
                                .build()
                        )
                    )
                    .setVurderingsdato(LocalDate.now())
                    .setVurderingAvBeregningsdato(
                        VurderingAvBeregningsdato.newBuilder().setTilstand("SØKNAD_MOTTATT").build()
                    )
                    .build()
            )
        ).build()
}

fun <T> awaitDatabase(timeoutMs: Long = 1_000, query: suspend () -> T?): T? = runBlocking {
    withTimeoutOrNull(timeoutMs) {
        val coldFlow = channelFlow {
            while (true) newSuspendedTransaction(Dispatchers.IO) {
                query()?.let {
                    send(it)
                }
            }
        }
        coldFlow.firstOrNull()
    }
}


fun <R> withTestApp(test: TestApplicationEngine.(mocks: Mocks) -> R): R = Mocks().use { mocks ->
    val externalConfig = mapOf(
        "H2_URL" to "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "DB_USERNAME" to "sa",
        "DB_PASSWORD" to "",
        "KAFKA_STREAMS_APPLICATION_ID" to "sink",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_SECURITY_ENABLED" to "false",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
        "KAFKA_CLIENT_ID" to "sink",
        "KAFKA_GROUP_ID" to "sink-1",
        "KAFKA_SCHEMA_REGISTRY" to mocks.kafka.schemaRegistryUrl,
        "KAFKA_SCHEMA_REGISTRY_USER" to "",
        "KAFKA_SCHEMA_REGISTRY_PASSWORD" to "",
    )

    return EnvironmentVariables(externalConfig).execute<R> {
        withTestApplication(
            { app(mocks.kafka) },
            { test(mocks) }
        )
    }
}

class Mocks : AutoCloseable {
    val kafka = KStreamsMock()

    override fun close() {
        transaction {
            exec("SET REFERENTIAL_INTEGRITY FALSE")
            exec("TRUNCATE TABLE søker")
            exec("SET REFERENTIAL_INTEGRITY TRUE")
        }
    }
}

class KStreamsMock : Kafka {
    lateinit var driver: TopologyTestDriver
    private lateinit var config: KafkaConfig

    override fun init(topology: Topology, config: KafkaConfig) {
        this.driver = TopologyTestDriver(topology, config.consumer + config.producer + testConfig)
        this.config = config
    }

    internal val schemaRegistryUrl: String by lazy { "mock://schema-registry/${UUID.randomUUID()}" }

    override fun close() = driver.close().also { MockSchemaRegistry.dropScope(schemaRegistryUrl) }
    override fun healthy(): Boolean = true

    fun <V : SpecificRecord> inputAvroTopic(name: String): TestInputTopic<String, V> {
        val serde = SpecificAvroSerde<V>().apply { configure(avroConfig, false) }
        return driver.createInputTopic(name, Serdes.String().serializer(), serde.serializer())
    }

    fun <V : SpecificRecord> outputAvroTopic(name: String): TestOutputTopic<String, V> {
        val serde = SpecificAvroSerde<V>().apply { configure(avroConfig, false) }
        return driver.createOutputTopic(name, Serdes.String().deserializer(), serde.deserializer())
    }

    private val testConfig = Properties().apply {
        this[StreamsConfig.STATE_DIR_CONFIG] = "build/kafka-streams/state"
        this[StreamsConfig.MAX_TASK_IDLE_MS_CONFIG] = StreamsConfig.MAX_TASK_IDLE_MS_DISABLED
    }

    private val avroConfig: Map<String, String>
        get() = mapOf(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to config.schemaRegistryUrl)
}

inline fun <reified V : Any> TestInputTopic<String, V>.produce(key: String, value: () -> V) = pipeInput(key, value())
