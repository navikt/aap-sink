package app

import kotlinx.coroutines.Dispatchers
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.kv
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

object Repo {
    private val secureLog = LoggerFactory.getLogger("secureLog")

    fun connect(config: DatabaseConfig) {
        Database.connect(url = config.url, user = config.username, password = config.password)
        Flyway.configure().dataSource(config.url, config.username, config.password).load().migrate()
    }

    suspend fun save(dao: DaoRecord) = newSuspendedTransaction(Dispatchers.IO) {
        addLogger(SqlInfoLogger)

        SøkerTable.insert { it.setValues(dao) }
    }

    suspend fun search(personident: String): List<DaoRecord> = newSuspendedTransaction(Dispatchers.IO) {
        addLogger(SqlInfoLogger)

        SøkerTable
            .select(SøkerTable.personident eq personident)
            .map {
                DaoRecord(
                    personident = it[SøkerTable.personident],
                    record = it[SøkerTable.record],
                    dtoVersion = it[SøkerTable.dtoVersion],
                    partition = it[SøkerTable.partition],
                    offset = it[SøkerTable.offset],
                    topic = it[SøkerTable.topic],
                    timestamp = it[SøkerTable.timestamp],
                    systemTimeMs = it[SøkerTable.systemTimeMs],
                    streamTimeMs = it[SøkerTable.streamTimeMs],
                )
            }
    }

    private fun InsertStatement<Number>.setValues(dao: DaoRecord) {
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

    private object SqlInfoLogger : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {

            secureLog.trace(
                context.expandArgs(TransactionManager.current()),
                context.kvPersonident(),
            )
        }

        private fun StatementContext.kvPersonident(): StructuredArgument? {
            val personident = if (statement.targets.any { it == SøkerTable }) {
                when (statement.type) {
                    StatementType.INSERT -> (statement as? InsertStatement<*>)?.getOrNull(SøkerTable.personident)
                    else -> null
                }
            } else null

            return personident?.let { kv("personident", it) }
        }
    }
}

object SøkerTable : Table() {
    val id = long("id").autoIncrement()
    val personident = varchar("personident", 11)
    val record = text("record")
    val dtoVersion = integer("dto_version").nullable()
    val partition = integer("kafka_partition")
    val offset = long("kafka_offset")
    val topic = varchar("kafka_topic", 50)
    val timestamp = long("kafka_timestamp_ms")
    val streamTimeMs = long("kafka_stream_time_ms")
    val systemTimeMs = long("kafka_system_time_ms")
}
