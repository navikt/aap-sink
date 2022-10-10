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
        fun create(resRow: ResultRow) = SøkerDao(
            personident = resRow[SøkerTable.personident],
            record = resRow[SøkerTable.record],
            dtoVersion = resRow[SøkerTable.dtoVersion],
            partition = resRow[SøkerTable.partition],
            offset = resRow[SøkerTable.offset],
            topic = resRow[SøkerTable.topic],
            timestamp = resRow[SøkerTable.timestamp],
            systemTimeMs = resRow[SøkerTable.systemTimeMs],
            streamTimeMs = resRow[SøkerTable.streamTimeMs],
        )

        fun transformFromRecord(): TransformDao<SøkerDao> = { key, record, version, metadata ->
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
