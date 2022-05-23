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
import org.slf4j.LoggerFactory
import java.sql.ResultSet

object Repo {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    private object SqlInfoLogger : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            secureLog.info(context.expandArgs(TransactionManager.current()))
        }
    }

    fun connect(config: DatabaseConfig) {
        Database.connect(url = config.url, user = config.username, password = config.password)
        Flyway.configure().dataSource(config.url, config.username, config.password).load().migrate()
    }

    suspend fun save(dao: DaoRecord) = newSuspendedTransaction(Dispatchers.IO) {
        addLogger(SqlInfoLogger)

        SøkerTable.insert { it.setValues(dao) }.also {
            secureLog.info("inserted row with id ${it[SøkerTable.id]} personident ${dao.personident}")
        }
    }

    suspend fun search(personident: String): List<ByteArray> = newSuspendedTransaction(Dispatchers.IO) {
        addLogger(SqlInfoLogger)

        SøkerTable.select(SøkerTable.personident eq personident)
            .onEach { secureLog.info("found row with id ${it[SøkerTable.id]} personident $personident") }
            .map { it[SøkerTable.record].toByteArray() }
    }

    private fun InsertStatement<Number>.setValues(dao: DaoRecord) {
        this[SøkerTable.personident] = dao.personident
        this[SøkerTable.record] = dao.record
        this[SøkerTable.partition] = dao.partition
        this[SøkerTable.offset] = dao.offset
        this[SøkerTable.topic] = dao.topic
        this[SøkerTable.timestamp] = dao.timestamp
        this[SøkerTable.streamTimeMs] = dao.streamTimeMs
        this[SøkerTable.systemTimeMs] = dao.systemTimeMs
    }
}

object SøkerTable : Table() {
    val id = long("id").autoIncrement()
    val personident = varchar("personident", 11)
    val record = text("record")
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val topic = varchar("kafka_topic", 50)
    val timestamp = long("kafka_timestamp_ms")
    val streamTimeMs = long("kafka_stream_time_ms")
    val systemTimeMs = long("kafka_system_time_ms")
}
