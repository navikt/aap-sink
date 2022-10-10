package app.kafka

import app.Dao
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.streams.processor.api.FixedKeyProcessor
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext
import org.apache.kafka.streams.processor.api.FixedKeyRecord

const val TOMBSTONE_VALUE = "tombstone"

internal class EnrichWithMetadata : FixedKeyProcessor<String, ByteArray?, Dao> {
    private val jackson: ObjectMapper = jacksonObjectMapper()
    private lateinit var context: FixedKeyProcessorContext<String, Dao>

    override fun init(context: FixedKeyProcessorContext<String, Dao>) {
        this.context = context
    }

    override fun process(record: FixedKeyRecord<String, ByteArray?>) {
        val metadata = context.recordMetadata().orElseThrow()

        val dao = Dao(
           personident = record.key(),
           record = record.value()?.decodeToString() ?: TOMBSTONE_VALUE,
           dtoVersion = record.value()?.let(jackson::readTree)?.get("version")?.takeUnless { it.isNull }?.intValue(),
           topic = metadata.topic(),
           partition = metadata.partition(),
           offset = metadata.offset(),
           timestamp = record.timestamp(),
           systemTimeMs = context.currentStreamTimeMs(),
           streamTimeMs = context.currentSystemTimeMs()
        )

        context.forward(record.withValue(dao))
    }
}
