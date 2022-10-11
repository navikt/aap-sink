package app

import no.nav.aap.kafka.serde.json.JsonSerde
import no.nav.aap.kafka.streams.Topic

internal data class KafkaDto (
    val topic: String,
    val data: String,
) {
    constructor(topic: Topic<ByteArray>): this(topic.name, "data")

    private val serializer = JsonSerde.jackson<KafkaDto>().serializer()

    fun toByteArray(): ByteArray = serializer.serialize(topic, this)
}

internal data class VersionedKafkaDto(
    val topic: String,
    val version: Int,
) {
    constructor(topic: Topic<ByteArray>, version: Int = 2): this(topic.name, version)

    private val serializer = JsonSerde.jackson<VersionedKafkaDto>().serializer()

    fun toByteArray(): ByteArray = serializer.serialize(topic, this)
}
