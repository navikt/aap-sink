package app

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.kafka.streams.KStreams
import no.nav.aap.kafka.streams.KafkaStreams
import no.nav.aap.kafka.streams.consume
import no.nav.aap.ktor.config.loadConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.ValueTransformerWithKey
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
import org.apache.kafka.streams.processor.ProcessorContext

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::app).start(wait = true)
}

fun Application.app(kafka: KStreams = KafkaStreams) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) { registry = prometheus }

    val config = loadConfig<Config>()

    Repo.connect(config.database)

    val topology = StreamsBuilder().apply {
        consume(Topics.søkere)
            .transformValues(ValueTransformerWithKeySupplier { RecordWithMetadataTransformer() })
            .foreach { _, dao ->
                runBlocking {
                    Repo.save(dao)
                }
            }
    }.build()

    kafka.connect(config.kafka, prometheus, topology)

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    routing {
        route("/actuator") {
            get("/metrics") {
                call.respond(prometheus.scrape())
            }
            get("/live") {
                val status = if (kafka.isLive()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "vedtak")
            }
            get("/ready") {
                val status = if (kafka.isReady()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
                call.respond(status, "vedtak")
            }
        }
    }
}

data class DaoRecord(
    val personident: String,
    val record: String,
    val partition: Int,
    val offset: Long,
    val topic: String,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
)

class RecordWithMetadataTransformer : ValueTransformerWithKey<String, ByteArray?, DaoRecord> {
    private lateinit var context: ProcessorContext

    override fun init(processorContext: ProcessorContext) = let { context = processorContext }
    override fun close() {}

    override fun transform(key: String, value: ByteArray?) = DaoRecord(
        personident = key,
        record = value?.decodeToString() ?: "tombstone",
        partition = context.partition(),
        offset = context.offset(),
        topic = context.topic(),
        timestamp = context.timestamp(),
        systemTimeMs = context.currentSystemTimeMs(),
        streamTimeMs = context.currentStreamTimeMs(),
    )
}