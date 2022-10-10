package app.meldeplikt

import app.kafka.TransformDao
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.InsertStatement

data class MeldepliktDao(
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
        stmt[MeldepliktTable.personident] = personident
        stmt[MeldepliktTable.record] = record
        stmt[MeldepliktTable.dtoVersion] = dtoVersion
        stmt[MeldepliktTable.partition] = partition
        stmt[MeldepliktTable.offset] = offset
        stmt[MeldepliktTable.topic] = topic
        stmt[MeldepliktTable.timestamp] = timestamp
        stmt[MeldepliktTable.streamTimeMs] = streamTimeMs
        stmt[MeldepliktTable.systemTimeMs] = systemTimeMs
    }

    companion object {
        fun create(rs: ResultRow) = MeldepliktDao(
            personident = rs[MeldepliktTable.personident],
            record = rs[MeldepliktTable.record],
            dtoVersion = rs[MeldepliktTable.dtoVersion],
            partition = rs[MeldepliktTable.partition],
            offset = rs[MeldepliktTable.offset],
            topic = rs[MeldepliktTable.topic],
            timestamp = rs[MeldepliktTable.timestamp],
            systemTimeMs = rs[MeldepliktTable.systemTimeMs],
            streamTimeMs = rs[MeldepliktTable.streamTimeMs],
        )

        fun transformFromRecord(): TransformDao<MeldepliktDao> = { key, record, version, metadata ->
            MeldepliktDao(
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
