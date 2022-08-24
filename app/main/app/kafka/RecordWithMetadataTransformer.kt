package app.kafka

import app.søker.SøkerDao
import app.vedtak.VedtakDao
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.streams.kstream.ValueTransformerWithKey
import org.apache.kafka.streams.kstream.ValueTransformerWithKeySupplier
import org.apache.kafka.streams.processor.ProcessorContext

typealias TransformDao<T> = (String, String, Int?, ProcessorContext) -> T

const val TOMBSTONE_VALUE = "tombstone"

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
        val record = value?.decodeToString() ?: TOMBSTONE_VALUE
        return toDao(key, record, version, context)
    }

    override fun close() {}
}

fun toSøkerDaoWithRecordMetadata() = ValueTransformerWithKeySupplier {
    RecordWithMetadataTransformer<SøkerDao>(SøkerDao.fromKafkaRecord())
}

fun toVedtakDaoWithRecordMetadata() = ValueTransformerWithKeySupplier {
    RecordWithMetadataTransformer<VedtakDao>(VedtakDao.fromKafkaRecord())
}
