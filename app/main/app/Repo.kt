package app

import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object Repo {
    private val log = LoggerFactory.getLogger("app")

    private object SqlInfoLogger : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            log.info(context.expandArgs(TransactionManager.current()))
        }
    }

    fun connect(config: DatabaseConfig) {
        if (config.h2Url.isNotBlank()) {
            log.warn("Using H2")
            Database.connect(url = config.h2Url, user = config.username, password = config.password)
            transaction { SchemaUtils.create(SøkerTable) }
        } else {
            Database.connect(url = config.url, user = config.username, password = config.password)
            Flyway.configure().dataSource(config.url, config.username, config.password).load().migrate()
        }
    }

    suspend fun save(personident: String, søker: ByteArray) = newSuspendedTransaction(Dispatchers.IO) {
        addLogger(SqlInfoLogger)

        SøkerTable.insert { it.setValues(personident, søker) }.also {
            log.info("inserted row with id ${it[SøkerTable.id]} personident $personident")
        }
    }

    suspend fun search(personident: String): List<ByteArray> = newSuspendedTransaction(Dispatchers.IO) {
        addLogger(SqlInfoLogger)

        SøkerTable.select(SøkerTable.personident eq personident)
            .onEach { log.info("found row with id ${it[SøkerTable.id]} personident $personident") }
            .map { it[SøkerTable.record].toByteArray() }
    }

    private fun InsertStatement<Number>.setValues(personident: String, søker: ByteArray) {
        this[SøkerTable.personident] = personident
        this[SøkerTable.record] = søker.decodeToString()
    }
}

object SøkerTable : Table() {
    val id = long("id").autoIncrement()
    val personident = varchar("personident", 11)
    val record = text("record")
}
