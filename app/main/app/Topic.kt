package app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.aap.avro.vedtak.v1.Soker

class Topic(config: KafkaConfig) {
    val name = "aap.sokere.v1"
    val valueSerde = SpecificAvroSerde<Soker>().apply {
        val conf = (config.ssl + config.schemaRegistry).map { it.key.toString() to it.value.toString() }
        configure(conf.toMap(), false)
    }
}
