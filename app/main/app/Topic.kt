package app

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.aap.avro.vedtak.v1.Soker
import org.apache.kafka.common.serialization.Serdes.ByteArraySerde

class Topic(config: KafkaConfig) {
    val name = "aap.sokere.v1"

    val byteArraySerde = ByteArraySerde().apply {
        val conf = (config.ssl + config.schemaRegistry).map { it.key.toString() to it.value.toString() }
        configure(conf.toMap(), false)
    }

    val avroSerde = SpecificAvroSerde<Soker>().apply {
        val conf = (config.ssl + config.schemaRegistry).map { it.key.toString() to it.value.toString() }
        configure(conf.toMap(), false)
    }
}
