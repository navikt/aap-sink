package app.meldeplikt

import app.exposed.SqlTraceLogger
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object MeldepliktRepo {

    fun save(dao: MeldepliktDao) = transaction {
        addLogger()

        MeldepliktTable.insert { dao.toInsertStatement(it) }
    }

    fun searchBy(personident: String): List<MeldepliktDao> = transaction {
        addLogger(SqlTraceLogger)

        MeldepliktTable
            .select(MeldepliktTable.personident eq personident)
            .map(MeldepliktDao::create)
    }

    fun lastBy(personident: String, column: (MeldepliktTable) -> Expression<*>): MeldepliktDao = transaction {
        addLogger(SqlTraceLogger)

        MeldepliktTable
            .select(MeldepliktTable.personident eq personident)
            .orderBy(column(MeldepliktTable), SortOrder.DESC)
            .limit(1)
            .map(MeldepliktDao::create)
            .single()
    }
}
