package app

import no.nav.aap.kafka.KafkaConfig

data class Config(
    val database: DatabaseConfig,
    val kafka: KafkaConfig,
)

data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
)
