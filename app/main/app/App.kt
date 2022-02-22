package app

import io.ktor.application.*
import io.ktor.metrics.micrometer.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.avro.vedtak.v1.Soker
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::app).start(wait = true)
}

private val log = LoggerFactory.getLogger("app")

fun Application.app(kafka: Kafka = KStreams()) {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) { registry = prometheus }

    val config = loadConfig<Config>()
    Repo.connect(config.database)
    val topic = Topic(config.kafka)
    val topology = createTopology(topic)
    kafka.init(topology, config.kafka)

    environment.monitor.subscribe(ApplicationStopping) {
        kafka.close()
    }

    routing {
        actuator(prometheus)
    }
}

fun createTopology(topic: Topic): Topology = StreamsBuilder().apply {
    stream(topic.name, Consumed.with(Serdes.StringSerde(), topic.valueSerde))
        .peek { k, v -> log.info("consumed $k $v") }
        .split()
        .branch({ _, value -> value == null }, logDeleted())
        .defaultBranch(saveRecords(topic))
}.build()

private fun logDeleted() = Branched.withConsumer<String, Soker> {
    it.foreach { key, _ -> log.info("found tombstone for personident $key") }
}

private fun saveRecords(topic: Topic) = Branched.withConsumer<String, Soker> {
    it.foreach { _, søker ->
        runBlocking {
            Repo.save(søker, topic)
        }
    }
}

fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") { call.respond(prometheus.scrape()) }
        get("/live") { call.respond("sink") }
        get("/ready") { call.respond("sink") }
    }
}