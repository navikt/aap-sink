package app

data class Dto(
    val personident: String,
    val record: ByteArray,
    val dtoVersion: Int?,
    val partition: Int,
    val offset: Long,
    val topic: String,
    val timestamp: Long,
    val systemTimeMs: Long,
    val streamTimeMs: Long,
) {
    companion object {
        fun from(dao: Dao): Dto =
            Dto(
                personident = dao.personident,
                record = dao.record.encodeToByteArray(),
                dtoVersion = dao.dtoVersion,
                partition = dao.partition,
                offset = dao.offset,
                topic = dao.topic,
                timestamp = dao.timestamp,
                systemTimeMs = dao.systemTimeMs,
                streamTimeMs = dao.streamTimeMs,
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dto

        if (personident != other.personident) return false
        if (!record.contentEquals(other.record)) return false
        if (dtoVersion != other.dtoVersion) return false
        if (partition != other.partition) return false
        if (offset != other.offset) return false
        if (topic != other.topic) return false
        if (timestamp != other.timestamp) return false
        if (systemTimeMs != other.systemTimeMs) return false
        if (streamTimeMs != other.streamTimeMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = personident.hashCode()
        result = 31 * result + record.contentHashCode()
        result = 31 * result + (dtoVersion ?: 0)
        result = 31 * result + partition
        result = 31 * result + offset.hashCode()
        result = 31 * result + topic.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + systemTimeMs.hashCode()
        result = 31 * result + streamTimeMs.hashCode()
        return result
    }
}
