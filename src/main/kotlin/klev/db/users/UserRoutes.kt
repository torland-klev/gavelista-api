package klev.db.users

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import klev.db.groups.GroupService
import klev.oauthUserId
import klev.routeId

class UserRoutes(
    private val userService: UserService,
    private val groupService: GroupService,
) {
    suspend fun get(call: ApplicationCall) {
        val id = call.oauthUserId()
        if (id != null) {
            call.respond(HttpStatusCode.OK, userService.fromIds(groupService.allMembersUserIsConnectedTo(id)))
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    suspend fun getById(call: ApplicationCall) {
        val oauthId = call.oauthUserId()
        val id = call.routeId("userId")
        if (id == null || id != oauthId) {
            userService.read(id)?.let {
                call.respond(HttpStatusCode.OK, it.copy(email = "", lastName = ""))
            } ?: call.respond(HttpStatusCode.NotFound)
        } else {
            userService.read(id)?.let {
                call.respond(HttpStatusCode.OK, it)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }

    suspend fun me(call: ApplicationCall) {
        userService.read(call.oauthUserId())?.let {
            call.respond(HttpStatusCode.OK, it)
        } ?: call.respond(HttpStatusCode.NotFound)
    }

    suspend fun updateById(call: ApplicationCall) {
        userService.read(call.oauthUserId())?.let {
            val partial = call.receive<PartialUser>()
            if (userService.update(it.id, partial) > 0) {
                call.respond(HttpStatusCode.OK, userService.read(it.id)!!)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        } ?: call.respond(HttpStatusCode.NotFound)
    }
}
