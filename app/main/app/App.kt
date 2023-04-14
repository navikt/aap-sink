package app

import app.kafka.Topics
import app.meldeplikt.MeldepliktRepo
import app.mottaker.MottakerRepo
import app.søker.SøkerRepository
import app.søknad.SøknadRepo
import app.vedtak.VedtakRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.aap.kafka.streams.v2.*
import no.nav.aap.kafka.streams.v2.processor.ProcessorMetadata
import no.nav.aap.ktor.config.loadConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::app).start(wait = true)
}

fun Application.app(kafka: Streams = KafkaStreams()) {
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

        get("/søker") {
            val antall = requireNotNull(call.parameters["antall"]) { "Mangler antall (1, 5) som query param" }
            val retning = enumValueOf<SortOrder>(
                requireNotNull(call.parameters["retning"]) { "Mangler retning (ASC, DESC) som query parm " }
            )

            val personident = requireNotNull(call.request.header("personident")) {
                "Request mangler personident som header"
            }

            val søker: List<Dto> = SøkerRepository.takeBy(
                personident = personident,
                take = antall.toInt(),
                direction = retning,
            ) {
                it.timestamp
            }.map(Dto::from)

            val søknad: List<Dto> = SøknadRepo.takeBy(
                personident = personident,
                take = antall.toInt(),
                direction = retning,
            ) {
                it.timestamp
            }.map(Dto::from)

           val vedtak: List<Dto> = VedtakRepository.takeBy(
                personident = personident,
                take = antall.toInt(),
                direction = retning,
            ) {
                it.timestamp
            }.map(Dto::from)

           val meldeplikt: List<Dto> = MeldepliktRepo.takeBy(
                personident = personident,
                take = antall.toInt(),
                direction = retning,
            ) {
                it.timestamp
            }.map(Dto::from)

           val mottakere: List<Dto> = MottakerRepo.takeBy(
                personident = personident,
                take = antall.toInt(),
                direction = retning,
            ) {
                it.timestamp
            }.map(Dto::from)

            val result = (søker + søknad + vedtak + meldeplikt + mottakere)
                .sortedBy { it.timestamp }.takeLast(antall.toInt()) // take vs takeLast basert på retning

            call.respond(result)
        }
    }
}

fun topology(): Topology = topology {
    val jackson: ObjectMapper = jacksonObjectMapper()

    fun consumeAndSave(topic: Topic<ByteArray>, save: (Dao) -> Unit){
        consume(topic){ key, value, metadata ->
            val dao = dao(key, value, jackson, metadata)
            save(dao)
        }
    }

    consumeAndSave(Topics.søkere, SøkerRepository::save)
    consumeAndSave(Topics.søknad, SøknadRepo::save)
    consumeAndSave(Topics.meldeplikt, MeldepliktRepo::save)
    consumeAndSave(Topics.mottakere, MottakerRepo::save)
    consumeAndSave(Topics.vedtak, VedtakRepository::save)
}

private fun dao(
    key: String,
    value: ByteArray?,
    jackson: ObjectMapper,
    metadata: ProcessorMetadata
) = Dao(
    personident = key,
    record = value?.decodeToString() ?: "tombstone",
    dtoVersion = value?.let(jackson::readTree)?.get("version")?.takeUnless { it.isNull }?.intValue(),
    topic = metadata.topic,
    partition = metadata.partition,
    offset = metadata.offset,
    timestamp = metadata.timestamp,
    systemTimeMs = metadata.streamTimeMs,
    streamTimeMs = metadata.systemTimeMs
)

fun Route.actuators(prometheus: PrometheusMeterRegistry, kafka: Streams) {
    route("/actuator") {
        get("/metrics") {
            call.respond(prometheus.scrape())
        }
        get("/live") {
            val status = if (kafka.live()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
            call.respond(status, "sink")
        }
        get("/ready") {
            val status = if (kafka.ready()) HttpStatusCode.OK else HttpStatusCode.InternalServerError
            call.respond(status, "sink")
        }
    }
}
