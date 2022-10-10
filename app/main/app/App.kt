package app

import app.kafka.EnrichWithMetadata
import app.kafka.Topics
import app.meldeplikt.MeldepliktRepo
import app.søker.SøkerRepository
import app.vedtak.VedtakRepository
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
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
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::app).start(wait = true)
}

fun Application.app(kafka: KStreams = KafkaStreams) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) { registry = prometheus }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    val config = loadConfig<Config>()

    Database.connect(
        url = config.database.url,
        user = config.database.username,
        password = config.database.password,
    )

    Flyway.configure()
        .dataSource(config.database.url, config.database.username, config.database.password)
        .load()
        .migrate()

    kafka.connect(
        config = config.kafka,
        registry = prometheus,
        topology = topology(),
    )

    Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    environment.monitor.subscribe(ApplicationStopping) { kafka.close() }

    routing {
        actuators(prometheus, kafka)
    }
}

fun topology(): Topology {
    val builder = StreamsBuilder()

    builder.consume(Topics.søkere)
        .processValues({ EnrichWithMetadata() })
        .foreach { _, dao -> SøkerRepository.save(dao) }

    builder.consume(Topics.vedtak)
        .processValues({ EnrichWithMetadata() })
        .foreach { _, dao -> VedtakRepository.save(dao) }

    builder.consume(Topics.meldeplikt)
        .processValues({ EnrichWithMetadata() })
        .foreach { _, dao -> MeldepliktRepo.save(dao) }

    return builder.build()
}

fun Route.actuators(prometheus: PrometheusMeterRegistry, kafka: KStreams) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }
        get("/live") {
            val status = if (kafka.isLive()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
            call.respond(status, "sink")
        }
        get("/ready") {
            val status = if (kafka.isReady()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
            call.respond(status, "sink")
        }
    }
}
