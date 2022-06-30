package app.exposed

import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.LoggerFactory

object SqlTraceLogger : SqlLogger {
    private val secureLog = LoggerFactory.getLogger("secureLog")

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

        return personident?.let { StructuredArguments.kv("personident", it) }
    }
}
