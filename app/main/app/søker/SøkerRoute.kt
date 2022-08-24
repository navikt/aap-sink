package app.routes

import app.søker.SøkerDao
import app.søker.SøkerRepository
import app.vedtak.VedtakDao
import app.vedtak.VedtakRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Route.søker() {
    route("/soker/{personident}/latest") {
        get {
            val personident = call.parameters.getOrFail("personident")

            val søker: SøkerDao = SøkerRepository.lastBy(personident) { it.timestamp }

            call.respond(søker)
        }
    }
}


