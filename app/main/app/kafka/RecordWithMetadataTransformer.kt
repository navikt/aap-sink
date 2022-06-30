package app.kafka

import app.exposed.SøkerDao
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.streams.kstream.ValueTransformerWithKey
import org.apache.kafka.streams.processor.ProcessorContext

typealias TransformDao<T> = (String, String, Int?, ProcessorContext) -> T

class RecordWithMetadataTransformer<T>(
    private val toDao: TransformDao<T>,
) : ValueTransformerWithKey<String, ByteArray?, T> {
    private val jackson: ObjectMapper = jacksonObjectMapper()

    private lateinit var context: ProcessorContext

    override fun init(processorContext: ProcessorContext) {
        context = processorContext
    }

    override fun transform(key: String, value: ByteArray?): T {
        val version: Int? = value?.let(jackson::readTree)?.get("version")?.takeUnless { it.isNull }?.intValue()
        val record = value?.decodeToString() ?: "tombstone"
        return toDao(key, record, version, context)
    }

    override fun close() {}
}

fun toSøkerDao(): TransformDao<SøkerDao> = { key, record, version, metadata ->
    SøkerDao(
        personident = key,
        record = record,
        dtoVersion = version,
        partition = metadata.partition(),
        offset = metadata.offset(),
        topic = metadata.topic(),
        timestamp = metadata.timestamp(),
        systemTimeMs = metadata.currentSystemTimeMs(),
        streamTimeMs = metadata.currentStreamTimeMs(),
    )
}
