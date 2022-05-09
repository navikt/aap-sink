package app

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
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.time.Duration
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AppTest {

    @Test
    fun `store records in database`() {
        Mocks().use { mocks ->
            EnvironmentVariables(containerProperties(mocks)).execute {
                testApplication {
                    application {
                        app(mocks.kafka).also {
                            val søkere = mocks.kafka.inputTopic(Topics.søkere)

                            søkere.produce("123") {
                                JsonSerde.jackson<TestSøker>().serializer().serialize(Topics.søkere.name, TestSøker())
                            }

                            val søker = awaitDatabase {
                                Repo.search("123")
                            }

                            assertNotNull(søker)
                            assertTrue(søker.size == 1)
                        }
                    }
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

private fun containerProperties(mocks: Mocks) = mapOf(
    "DB_HOST" to mocks.postgres.host,
    "DB_PORT" to mocks.postgres.firstMappedPort.toString(),
    "DB_DATABASE" to mocks.postgres.databaseName,
    "DB_USERNAME" to mocks.postgres.username,
    "DB_PASSWORD" to mocks.postgres.password,
    "KAFKA_STREAMS_APPLICATION_ID" to "sink",
    "KAFKA_BROKERS" to "mock://kafka",
    "KAFKA_TRUSTSTORE_PATH" to "",
    "KAFKA_SECURITY_ENABLED" to "false",
    "KAFKA_KEYSTORE_PATH" to "",
    "KAFKA_CREDSTORE_PASSWORD" to "",
    "KAFKA_CLIENT_ID" to "sink",
)

class Mocks : AutoCloseable {
    val kafka = KafkaStreamsMock()
    val postgres = PostgreSQLContainer<Nothing>("postgres:14")

    init {
        postgres.withStartupTimeout(Duration.ofSeconds(2))
        postgres.start()
    }

    override fun close() {
        postgres.close()
    }
}

inline fun <reified V : Any> TestInputTopic<String, V>.produce(key: String, value: () -> V) = pipeInput(key, value())
