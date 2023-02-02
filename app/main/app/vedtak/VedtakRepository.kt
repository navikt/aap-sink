package app.vedtak

import app.Dao
import app.exposed.SqlTraceLogger
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object VedtakRepository {
    fun save(dao: Dao) = transaction {
        addLogger(SqlTraceLogger)

        VedtakTable.insert {
            it[personident] = dao.personident
            it[record] = dao.record
            it[dtoVersion] = dao.dtoVersion
            it[partition] = dao.partition
            it[offset] = dao.offset
            it[topic] = dao.topic
            it[timestamp] = dao.timestamp
            it[streamTimeMs] = dao.streamTimeMs
            it[systemTimeMs] = dao.systemTimeMs
        }
    }

    fun search(personident: String): List<Dao> = transaction {
        addLogger(SqlTraceLogger)

        VedtakTable
            .select(VedtakTable.personident eq personident)
            .map(::toDao)
    }

    fun takeBy(personident: String, take: Int, column: (VedtakTable) -> Expression<*>): Dao = transaction {
        addLogger(SqlTraceLogger)

        VedtakTable
            .select(VedtakTable.personident eq personident)
            .orderBy(column(VedtakTable), SortOrder.DESC)
            .limit(take)
            .map(::toDao)
            .single()
    }

    private fun toDao(rs: ResultRow) = Dao(
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
}
