package app.exposed

import app.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

object Repo {
    fun connect(config: DatabaseConfig) {
        Database.connect(url = config.url, user = config.username, password = config.password)
        Flyway.configure().dataSource(config.url, config.username, config.password).load().migrate()
    }

    fun save(dao: SøkerDao) = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable.insert { stmt -> stmt.setValues(dao) }
    }

    fun search(personident: String): List<SøkerDao> = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .map(::toDaoRecord)
    }

    fun lastBy(personident: String, column: (SøkerTable) -> Expression<*>): SøkerDao = transaction {
        addLogger(SqlTraceLogger)

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .orderBy(column(SøkerTable), SortOrder.DESC)
            .limit(1)
            .map(::toDaoRecord)
            .single()
    }
}

private fun toDaoRecord(rs: ResultRow): SøkerDao = SøkerDao(
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

private fun InsertStatement<Number>.setValues(dao: SøkerDao) {
    this[SøkerTable.personident] = dao.personident
    this[SøkerTable.record] = dao.record
    this[SøkerTable.dtoVersion] = dao.dtoVersion
    this[SøkerTable.partition] = dao.partition
    this[SøkerTable.offset] = dao.offset
    this[SøkerTable.topic] = dao.topic
    this[SøkerTable.timestamp] = dao.timestamp
    this[SøkerTable.streamTimeMs] = dao.streamTimeMs
    this[SøkerTable.systemTimeMs] = dao.systemTimeMs
}
