package app.kafka

import no.nav.aap.kafka.streams.Topic
import org.apache.kafka.common.serialization.Serdes.ByteArraySerde

object Topics {
    val søkere = Topic("aap.sokere.v1", ByteArraySerde())
    val søknad = Topic("aap.soknad-sendt.v1", ByteArraySerde())
    val vedtak = Topic("aap.vedtak.v1", ByteArraySerde())
    val meldeplikt = Topic("aap.meldeplikt.v1", ByteArraySerde())
    val mottakere = Topic("aap.mottakere.v1", ByteArraySerde())
}
