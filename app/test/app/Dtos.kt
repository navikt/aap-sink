package app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.aap.kafka.streams.v2.Topic


internal data class KafkaDto (
    val topic: String,
    val data: String,
) {
    constructor(topic: Topic<ByteArray>): this(topic.name, "data")

    private val jackson: ObjectMapper = jacksonObjectMapper()

    fun toByteArray(): ByteArray = jackson.writeValueAsBytes(this)
}

internal data class VersionedKafkaDto(
    val topic: String,
    val version: Int,
) {
    constructor(topic: Topic<ByteArray>, version: Int = 2): this(topic.name, version)

    private val jackson: ObjectMapper = jacksonObjectMapper()

    fun toByteArray(): ByteArray = jackson.writeValueAsBytes(this)
}
