package app.søknad

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

object SøknadRepo {
    fun save(dao: Dao) = transaction {
        addLogger(SqlTraceLogger)

        SøknadTable.insert {
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

        SøknadTable
            .select(SøknadTable.personident eq personident)
            .map(::toDao)
    }

    fun takeBy(personident: String, take: Int, direction: SortOrder, column: (SøknadTable) -> Expression<*>): List<Dao> = transaction {
        addLogger(SqlTraceLogger)

        SøknadTable
            .select(SøknadTable.personident eq personident)
            .orderBy(column(SøknadTable), direction)
            .limit(take)
            .map(SøknadRepo::toDao)
    }

    fun lastBy(personident: String, take: Int, column: (SøknadTable) -> Expression<*>): Dao = transaction {
        addLogger(SqlTraceLogger)

        SøknadTable
            .select(SøknadTable.personident eq personident)
            .orderBy(column(SøknadTable), SortOrder.DESC)
            .limit(take)
            .map(::toDao)
            .single()
    }

    private fun toDao(rs: ResultRow) = Dao(
        personident = rs[SøknadTable.personident],
        record = rs[SøknadTable.record],
        dtoVersion = rs[SøknadTable.dtoVersion],
        partition = rs[SøknadTable.partition],
        offset = rs[SøknadTable.offset],
        topic = rs[SøknadTable.topic],
        timestamp = rs[SøknadTable.timestamp],
        systemTimeMs = rs[SøknadTable.systemTimeMs],
        streamTimeMs = rs[SøknadTable.streamTimeMs],
    )
}


