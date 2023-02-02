package app.mottaker

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

object MottakerRepo {

    fun save(dao: Dao) = transaction {
        addLogger()

        MottakerTable.insert {
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

        MottakerTable
            .select(MottakerTable.personident eq personident)
            .map(::toDao)
    }

    fun takeBy(personident: String, take: Int, column: (MottakerTable) -> Expression<*>): Dao = transaction {
        addLogger(SqlTraceLogger)

        MottakerTable
            .select(MottakerTable.personident eq personident)
            .orderBy(column(MottakerTable), SortOrder.DESC)
            .limit(take)
            .map(::toDao)
            .single()
    }

    private fun toDao(rs: ResultRow) = Dao(
        personident = rs[MottakerTable.personident],
        record = rs[MottakerTable.record],
        dtoVersion = rs[MottakerTable.dtoVersion],
        partition = rs[MottakerTable.partition],
        offset = rs[MottakerTable.offset],
        topic = rs[MottakerTable.topic],
        timestamp = rs[MottakerTable.timestamp],
        systemTimeMs = rs[MottakerTable.systemTimeMs],
        streamTimeMs = rs[MottakerTable.streamTimeMs],
    )
}
