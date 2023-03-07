package app.meldeplikt

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

object MeldepliktRepo {

    fun save(dao: Dao) = transaction {
        addLogger()

        MeldepliktTable.insert {
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

    fun searchBy(personident: String): List<Dao> = transaction {
        addLogger(SqlTraceLogger)

        MeldepliktTable
            .select(MeldepliktTable.personident eq personident)
            .map(::toDao)
    }

    fun takeBy(personident: String, take: Int, direction: SortOrder, column: (MeldepliktTable) -> Expression<*>): List<Dao> = transaction {
        addLogger(SqlTraceLogger)

        MeldepliktTable
            .select(MeldepliktTable.personident eq personident)
            .orderBy(column(MeldepliktTable), direction)
            .limit(take)
            .map(::toDao)
    }

    private fun toDao(rs: ResultRow) = Dao(
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
}
