package app.meldeplikt

import org.jetbrains.exposed.sql.Table

object MeldepliktTable : Table() {
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
