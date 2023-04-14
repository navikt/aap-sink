package app

import app.kafka.Topics
import app.meldeplikt.MeldepliktRepo
import app.mottaker.MottakerRepo
import app.søker.SøkerRepository
import app.søknad.SøknadRepo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.aap.kafka.streams.v2.test.StreamsMock
import org.apache.kafka.streams.TestInputTopic
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy
import java.time.Duration
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AppTest {
    private lateinit var mocks: Mocks

    @BeforeAll
    fun setupMocks() {
        mocks = Mocks()
    }

    @AfterAll
    fun closeMocks() {
        mocks.close()
    }

    @Test
    fun `can save records in database`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.søkere)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        KafkaDto(Topics.søkere).toByteArray()
                    }

                    val søker = awaitDatabase {
                        SøkerRepository.search(personident)
                    }?.singleOrNull()

                    assertNotNull(søker)
                    assertContentEquals(KafkaDto(Topics.søkere).toByteArray(), søker.record.toByteArray())
                }
            }

            runBlocking {
                val live = client.get("/actuator/live")
                assertEquals(HttpStatusCode.OK, live.status)

                val ready = client.get("/actuator/ready")
                assertEquals(HttpStatusCode.OK, ready.status)

                val metrics = client.get("/actuator/metrics")
                assertEquals(HttpStatusCode.OK, metrics.status)
                assertNotNull(metrics.bodyAsText())
            }
        }
    }

    @Test
    fun `can save tombstones in database`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.søkere)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        KafkaDto(Topics.søkere).toByteArray()
                    }

                    topic.tombstone(personident)

                    val søkere = awaitDatabase {
                        SøkerRepository.search(personident)
                    }

                    requireNotNull(søkere) { "søker $personident skal ligger i datbase" }
                    assertEquals(2, søkere.size)
                }
            }
        }
    }

    @Test
    fun `can save dto without version`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.søkere)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        KafkaDto(Topics.søkere).toByteArray()
                    }

                    val søker = awaitDatabase {
                        SøkerRepository.search(personident)
                    }?.singleOrNull()

                    requireNotNull(søker) { "søker $personident skal ligger i datbase" }
                    assertNull(søker.dtoVersion)
                }
            }
        }
    }

    @Test
    fun `can save dto with version`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.søkere)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        VersionedKafkaDto(Topics.søkere).toByteArray()
                    }

                    val søker = awaitDatabase {
                        SøkerRepository.search(personident)
                    }?.singleOrNull()

                    requireNotNull(søker) { "søker $personident skal ligger i datbase" }
                    assertEquals(2, søker.dtoVersion)
                }
            }
        }
    }

    @Test
    fun `can save søknad`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.søknad)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        KafkaDto(Topics.søknad).toByteArray()
                    }

                    val søknad = awaitDatabase {
                        SøknadRepo.search(personident)
                    }?.singleOrNull()

                    requireNotNull(søknad) { "søknad $personident skal ligger i datbase" }
                }
            }
        }
    }

    @Test
    fun `can save meldeplikt`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val meldepliktTopic = mocks.kafka.testTopic(Topics.meldeplikt)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    meldepliktTopic.produce(personident) {
                        KafkaDto(Topics.meldeplikt).toByteArray()
                    }

                    val meldeplikt = awaitDatabase {
                        MeldepliktRepo.searchBy(personident)
                    }?.singleOrNull()

                    requireNotNull(meldeplikt) { "meldeplikt $personident skal ligger i datbase" }
                }
            }
        }
    }

    @Test
    fun `can save mottaker`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.mottakere)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        KafkaDto(Topics.mottakere).toByteArray()
                    }

                    val mottaker = awaitDatabase {
                        MottakerRepo.searchBy(personident)
                    }?.singleOrNull()

                    requireNotNull(mottaker) { "mottaker $personident skal ligger i datbase" }
                }
            }
        }
    }

    @Test
    fun `can find last by timstamp`() {
        testApplication {
            environment { config = mocks.applicationConfig() }
            application {
                app(mocks.kafka).also {
                    val topic = mocks.kafka.testTopic(Topics.søkere)

                    val personident = Random.nextInt(Integer.MAX_VALUE).toString()

                    topic.produce(personident) {
                        KafkaDto(Topics.søkere).toByteArray()
                    }

                    val søker = awaitDatabase {
                        SøkerRepository.takeBy(personident, 1, SortOrder.DESC) { it.timestamp }.single()
                    }

                    requireNotNull(søker) { "søker $personident skal ligger i datbase" }

                    val expected = KafkaDto(Topics.søkere)
                    val actual = jacksonObjectMapper().readValue<KafkaDto>(søker.record)
                    assertEquals(expected, actual)
                    println(jacksonObjectMapper().writeValueAsString(listOf(søker)))
                }
            }
        }
    }

//    @Test
//    fun `søker route respond lastest søker by personident`() {
//        testApplication {
//            environment { config = mocks.applicationConfig() }
//            application {
//                app(mocks.kafka).also {
//                    val søkereTopic = mocks.kafka.testTopic(Topics.søkere)
//                    val serializer = JsonSerde.jackson<TestSøker>().serializer()
//                    val ident = "1234"
//                    søkereTopic.produce(ident) { serializer.serialize(Topics.søkere.name, TestSøker(ident)) }
//                }
//            }
//
//            val client = createClient { install(ContentNegotiation) { jackson() } }
//            val søkerDao = client
//                .get("soker/1234/latest") { contentType(ContentType.Application.Json) }
//                .body<SøkerDao>()
//
//            val expected = TestSøker("1234")
//            val actual = jacksonObjectMapper().readValue<TestSøker>(søkerDao.record)
//            assertEquals(expected, actual)
//        }
//    }
}

private fun <T> awaitDatabase(timeoutMs: Long = 1_000, query: suspend () -> T?): T? = runBlocking {
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

class Mocks : AutoCloseable {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:14").apply {
        withStartupCheckStrategy(MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(5)))
        start()
    }

    val kafka = StreamsMock()

    fun applicationConfig() = MapApplicationConfig(
        "DB_HOST" to postgres.host,
        "DB_PORT" to postgres.firstMappedPort.toString(),
        "DB_DATABASE" to postgres.databaseName,
        "DB_USERNAME" to postgres.username,
        "DB_PASSWORD" to postgres.password,
        "KAFKA_STREAMS_APPLICATION_ID" to "sink",
        "KAFKA_BROKERS" to "mock://kafka",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_SECURITY_ENABLED" to "false",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
    )

    override fun close() {
        postgres.close()
    }
}

inline fun <reified V : Any> TestInputTopic<String, V>.produce(key: String, value: () -> V) = pipeInput(key, value())
private fun <V> TestInputTopic<String, V>.tombstone(key: String) = pipeInput(key, null)
