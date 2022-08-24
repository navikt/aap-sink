package app.vedtak

import app.kafka.TransformDao
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.InsertStatement

data class VedtakDao(
    val personident: String,
    val record: String,
    val dtoVersion: Int?,
    val partition: Int,
    val offset: Long,
    val topic: String,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
) {
    fun toInsertStatement(stmt: InsertStatement<Number>) {
        stmt[VedtakTable.personident] = personident
        stmt[VedtakTable.record] = record
        stmt[VedtakTable.dtoVersion] = dtoVersion
        stmt[VedtakTable.partition] = partition
        stmt[VedtakTable.offset] = offset
        stmt[VedtakTable.topic] = topic
        stmt[VedtakTable.timestamp] = timestamp
        stmt[VedtakTable.streamTimeMs] = streamTimeMs
        stmt[VedtakTable.systemTimeMs] = systemTimeMs
    }

    companion object {
        fun toKafkaRecord(rs: ResultRow) = VedtakDao(
            personident = rs[VedtakTable.personident],
            record = rs[VedtakTable.record],
            dtoVersion = rs[VedtakTable.dtoVersion],
            partition = rs[VedtakTable.partition],
            offset = rs[VedtakTable.offset],
            topic = rs[VedtakTable.topic],
            timestamp = rs[VedtakTable.timestamp],
            systemTimeMs = rs[VedtakTable.systemTimeMs],
            streamTimeMs = rs[VedtakTable.streamTimeMs],
        )

        fun fromKafkaRecord(): TransformDao<VedtakDao> = { key, record, version, metadata ->
            VedtakDao(
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
    }
}
