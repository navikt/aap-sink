package app.søker

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

object SøkerRepository {
    fun save(dao: Dao) = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable.insert {
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

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .map(::toDao)
    }

    fun takeBy(personident: String, take: Int, direction: SortOrder, column: (SøkerTable) -> Expression<*>): List<Dao> = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .orderBy(column(SøkerTable), direction)
            .limit(take)
            .map(::toDao)
    }

    private fun toDao(rs: ResultRow) = Dao(
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
}


