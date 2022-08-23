package app.routes

import app.exposed.SøkerDao
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Route.søker() {
    route("/soker/{personident}/latest") {
        get {
            val personident = call.parameters.getOrFail("personident")

//            val søker: SøkerDao = Repo.lastBy(personident) { it.timestamp }

            call.respond(SøkerDao(personident, "", null, 0, 0, "", 0, 0, 0))
        }
    }
}
