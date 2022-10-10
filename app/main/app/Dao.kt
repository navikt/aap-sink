package app

data class Dao(
    val personident: String,
    val record: String,
    val dtoVersion: Int?,
    val partition: Int,
    val offset: Long,
    val topic: String,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
)
