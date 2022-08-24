package app.vedtak

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Route.vedtak() {
    route("/vedtak/{personident}/latest") {
        get {
            val personident = call.parameters.getOrFail("personident")

            val søker: VedtakDao = VedtakRepository.lastBy(personident) { it.timestamp }

            call.respond(søker)
        }
    }
}