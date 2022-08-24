package app.vedtak

import app.exposed.SqlTraceLogger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object VedtakRepository {
    fun save(dao: VedtakDao) = transaction {
        addLogger(SqlTraceLogger)

        VedtakTable.insert { dao.toInsertStatement(it) }
    }

    fun search(personident: String): List<VedtakDao> = transaction {
        addLogger(SqlTraceLogger)

        VedtakTable
            .select(VedtakTable.personident eq personident)
            .map(VedtakDao.Companion::toKafkaRecord)
    }

    fun lastBy(personident: String, column: (VedtakTable) -> Expression<*>): VedtakDao = transaction {
        addLogger(SqlTraceLogger)

        VedtakTable
            .select(VedtakTable.personident eq personident)
            .orderBy(column(VedtakTable), SortOrder.DESC)
            .limit(1)
            .map(VedtakDao.Companion::toKafkaRecord)
            .single()
    }
}
