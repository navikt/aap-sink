package app

import no.nav.aap.kafka.streams.KStreamsConfig

data class Config(
    val database: DatabaseConfig,
    val kafka: KStreamsConfig,
)

data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
)
