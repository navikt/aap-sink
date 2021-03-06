package app

import app.exposed.Repo
import app.exposed.SøkerDao
import app.kafka.RecordWithMetadataTransformer
import app.kafka.Topics
import app.kafka.toSøkerDao
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.kafka.streams.KStreams
import no.nav.aap.kafka.streams.KafkaStreams
import no.nav.aap.kafka.streams.extension.consume
import no.nav.aap.ktor.config.loadConfig
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::app).start(wait = true)
}

fun Application.app(kafka: KStreams = KafkaStreams) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) { registry = prometheus }

    val config = loadConfig<Config>()

    Repo.connect(config.database)
    kafka.connect(config.kafka, prometheus, topology())

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    routing {
        actuators(prometheus, kafka)
    }
}

fun topology(): Topology = StreamsBuilder().apply {
    consume(Topics.søkere)
        .transformValues(ValueTransformerWithKeySupplier { RecordWithMetadataTransformer<SøkerDao>(toSøkerDao()) })
        .foreach { _, dao -> Repo.save(dao) }
}.build()

fun Route.actuators(prometheus: PrometheusMeterRegistry, kafka: KStreams) {
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
