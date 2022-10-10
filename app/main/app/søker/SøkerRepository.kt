package app.søker

import app.exposed.SqlTraceLogger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object SøkerRepository {
    fun save(dao: SøkerDao) = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable.insert { dao.toInsertStatement(it) }
    }

    fun search(personident: String): List<SøkerDao> = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .map(SøkerDao.Companion::create)
    }

    fun lastBy(personident: String, column: (SøkerTable) -> Expression<*>): SøkerDao = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .orderBy(column(SøkerTable), SortOrder.DESC)
            .limit(1)
            .map(SøkerDao.Companion::create)
            .single()
    }
}


