package app

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KafkaStreams.State.*
import org.apache.kafka.streams.Topology

interface Kafka : AutoCloseable {
    fun init(topology: Topology, config: KafkaConfig)
    fun healthy(): Boolean
}

class KStreams : Kafka {
    private lateinit var streams: KafkaStreams

    override fun init(topology: Topology, config: KafkaConfig) {
        streams = KafkaStreams(topology, config.consumer + config.producer)
        streams.start()
    }

    override fun healthy(): Boolean = streams.state() in listOf(CREATED, RUNNING, REBALANCING)
    override fun close() = streams.close()
}