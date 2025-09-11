package klev.db.wishes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import klev.db.groups.groupsToWishes.GroupsToWishesService
import klev.oauthUserId
import klev.routeId

class WishesRoutes(
    private val wishesService: WishesService,
    private val groupsToWishesService: GroupsToWishesService,
) {
    suspend fun patch(call: ApplicationCall) {
        val id = call.routeId()
        val userId = call.oauthUserId()
        val partialWish = call.receive<PartialWish>()
        val updated = wishesService.update(id, userId, partialWish)
        if (updated != null) {
            call.respond(HttpStatusCode.OK, updated)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    suspend fun delete(call: ApplicationCall) {
        val id = call.routeId()
        val userId = call.oauthUserId()
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized)
        } else if (id == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val rowsDeleted = wishesService.delete(id, userId)
            if (rowsDeleted > 0) {
                call.respond(HttpStatusCode.OK, rowsDeleted)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

    suspend fun getId(call: ApplicationCall) {
        val found =
            wishesService.read(
                id = call.routeId(),
                userId = call.oauthUserId(),
            )
        if (found != null) {
            call.respond(HttpStatusCode.OK, found)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    suspend fun post(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val partialWish = call.receive<PartialWish>()
            if (partialWish.title == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing required field title")
            } else {
                call.respond(
                    HttpStatusCode.Created,
                    wishesService.createByPartial(partial = partialWish, userId = userId),
                )
            }
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }

    suspend fun allUserHasReadAccessTo(call: ApplicationCall) {
        call.respond(wishesService.allUserHasReadAccessTo(call.oauthUserId()).toSet())
    }

    suspend fun postForGroup(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val groupId = call.routeId("groupId") ?: call.respond(HttpStatusCode.BadRequest, "Missing required field groupId")
            val partialWish = call.receive<PartialWish>().copy(groupId = groupId.toString(), visibility = WishVisibility.GROUP.name)
            if (partialWish.title == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing required field title")
            } else {
                call.respond(
                    HttpStatusCode.Created,
                    wishesService.createByPartial(partial = partialWish, userId = userId),
                )
            }
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }

    suspend fun postForEvent(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val eventId = call.routeId("eventId") ?: call.respond(HttpStatusCode.BadRequest, "Missing required field groupId")
            val partialWish = call.receive<PartialWish>().copy(eventId = eventId.toString(), visibility = WishVisibility.EVENT.name)
            if (partialWish.title == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing required field title")
            } else {
                call.respond(
                    HttpStatusCode.Created,
                    wishesService.createByPartial(partial = partialWish, userId = userId),
                )
            }
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }

    suspend fun allUserHasCreated(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val wishCreator = call.routeId("userId")
            if (wishCreator == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing required field userId")
            } else {
                val wishes = wishesService.allUserHasReadAccessTo(userId).filter { it.userId == wishCreator }
                call.respond(HttpStatusCode.OK, wishes.toSet())
            }
        }
    }

    suspend fun getGroupsForWish(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val found =
                wishesService.read(
                    id = call.routeId(),
                    userId = call.oauthUserId(),
                )
            if (found != null) {
                val groupIds = groupsToWishesService.allByWish(found.id)
                call.respond(HttpStatusCode.OK, groupIds.map { it.groupId.toString() })
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }
}
