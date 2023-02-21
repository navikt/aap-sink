package app

import no.nav.aap.kafka.streams.v2.config.StreamsConfig


data class Config(
    val database: DatabaseConfig,
    val kafka: StreamsConfig,
)

data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
)
