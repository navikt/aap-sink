package app

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import no.nav.aap.kafka.serde.json.JsonSerde
import no.nav.aap.kafka.streams.test.KafkaStreamsMock
import org.apache.kafka.streams.TestInputTopic
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class AppTest {

    @Test
    fun `store records in database`() {
        Mocks().use { mocks ->
            testApplication {
                environment { config = mocks.applicationConfig() }
                application {
                    app(mocks.kafka).also {
                        val søkereTopic = mocks.kafka.inputTopic(Topics.søkere)
                        val testSerde = JsonSerde.jackson<TestSøker>()

                        søkereTopic.produce("123") {
                            testSerde.serializer().serialize(Topics.søkere.name, TestSøker())
                        }

                        val søkere = awaitDatabase {
                            Repo.search("123")
                        }

                        assertNotNull(søkere)
                        val søker = testSerde.deserializer().deserialize(Topics.søkere.name, søkere.single())
                        assertEquals(TestSøker(), søker)
                    }
                }
            }
        }
    }

    @Test
    fun `actuators available`() {
        Mocks().use { mocks ->
            testApplication {
                environment { config = mocks.applicationConfig() }
                application { app(mocks.kafka) }

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
    }
}

private data class TestSøker(
    val personident: String = "123",
    val status: String = "Mottatt",
)

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
        // DB connection is not ready before approximately 2 seconds.
        withStartupCheckStrategy(MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(2)))
    }

    init {
        postgres.start()
    }

    val kafka = KafkaStreamsMock()

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
        "KAFKA_CLIENT_ID" to "sink",
    )

    override fun close() {
        postgres.close()
    }
}

inline fun <reified V : Any> TestInputTopic<String, V>.produce(key: String, value: () -> V) = pipeInput(key, value())
