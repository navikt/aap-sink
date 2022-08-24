package app.søker

import app.kafka.TransformDao
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.InsertStatement

data class SøkerDao(
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
        stmt[SøkerTable.personident] = personident
        stmt[SøkerTable.record] = record
        stmt[SøkerTable.dtoVersion] = dtoVersion
        stmt[SøkerTable.partition] = partition
        stmt[SøkerTable.offset] = offset
        stmt[SøkerTable.topic] = topic
        stmt[SøkerTable.timestamp] = timestamp
        stmt[SøkerTable.streamTimeMs] = streamTimeMs
        stmt[SøkerTable.systemTimeMs] = systemTimeMs
    }

    companion object {
        fun toKafkaRecord(rs: ResultRow) = SøkerDao(
            personident = rs[SøkerTable.personident],
            record = rs[SøkerTable.record],
            dtoVersion = rs[SøkerTable.dtoVersion],
            partition = rs[SøkerTable.partition],
            offset = rs[SøkerTable.offset],
            topic = rs[SøkerTable.topic],
            timestamp = rs[SøkerTable.timestamp],
            systemTimeMs = rs[SøkerTable.systemTimeMs],
            streamTimeMs = rs[SøkerTable.streamTimeMs],
        )

        fun fromKafkaRecord(): TransformDao<SøkerDao> = { key, record, version, metadata ->
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
    }
}
