package app

import kotlinx.coroutines.Dispatchers
import no.nav.aap.avro.vedtak.v1.Soker
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

object Repo {
    private val log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    fun connect(config: DatabaseConfig) {
        Database.connect(url = config.url, user = config.username, password = config.password)
        transaction {
            SchemaUtils.create(SøkerTable)
        }
    }

    suspend fun save(avro: Soker, topic: Topic) = newSuspendedTransaction(Dispatchers.IO) {
        val insertedId = SøkerTable.insert {
            it.setValues(avro, topic)
        }

        log.info("inserted row with id ${insertedId[SøkerTable.id]}")
    }

    suspend fun search(personident: String, topic: Topic): List<Soker> = newSuspendedTransaction(Dispatchers.IO) {
        SøkerTable.select(SøkerTable.personIdent eq personident)
            .onEach { log.info("found row with id ${it[SøkerTable.id]}") }
            .map {
                val record = it[SøkerTable.record].encodeToByteArray()
                topic.valueSerde.deserializer().deserialize(topic.name, record)
            }
    }

    private fun InsertStatement<Number>.setValues(søker: Soker, topic: Topic) {
        val serialized: ByteArray = topic.valueSerde.serializer().serialize(topic.name, søker)
        this[SøkerTable.personIdent] = søker.personident
        this[SøkerTable.record] = serialized.decodeToString()
    }
}

object SøkerTable : Table("SOKERE") {
    val id = long("id").autoIncrement()
    val personIdent = varchar("personident", 11)
    val record = text("record")
}
