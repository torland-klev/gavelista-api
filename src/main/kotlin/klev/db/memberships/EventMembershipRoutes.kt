package klev.db.memberships

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import klev.db.events.EventService
import klev.db.users.UserService
import klev.oauthUserId
import klev.routeId

class EventMembershipRoutes(
    private val eventService: EventService,
    private val userService: UserService,
    private val eventMembershipService: EventMembershipService,
) {
    suspend fun allByEvent(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val event = eventService.getIfHasReadAccess(eventId = eventId, userId = userId)
        if (event == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val memberships = eventMembershipService.allByEvent(eventId = event.id)
            val users = memberships.map { userService.read(it.userId) }
            call.respond(HttpStatusCode.OK, users)
        }
    }

    suspend fun deleteIfCanAdmin(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val memberId = call.routeId("memberId")
        val event = eventService.getIfCanAdmin(eventId = eventId, userId = userId)
        if (event == null || memberId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val deleteSuccesses =
                eventMembershipService
                    .allByEvent(eventId = event.id)
                    .filter {
                        it.userId == memberId && it.eventId == event.id && it.role != MembershipRole.OWNER
                    }.map { eventMembershipService.delete(it.id) }
            if (deleteSuccesses.isEmpty() || deleteSuccesses.none { it }) {
                call.respond(HttpStatusCode.NotFound)
            } else if (deleteSuccesses.all { it }) {
                call.respond(HttpStatusCode.OK)
            } else if (deleteSuccesses.any { it }) {
                call.respond(HttpStatusCode.PartialContent)
            }
        }
    }

    suspend fun get(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val memberId = call.routeId("memberId")
        val event = eventService.getIfCanAdmin(eventId = eventId, userId = userId)
        if (event == null || memberId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = eventMembershipService.byEventAndUser(eventId = event.id, userId = memberId)
            if (membership == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, membership)
            }
        }
    }

    suspend fun addUserToEvent(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val memberId = call.routeId("memberId")
        val event = eventService.getIfCanAdmin(eventId = eventId, userId = userId)
        if (memberId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing member ID")
        } else if (event == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership =
                eventMembershipService.byEventAndUser(eventId = event.id, userId = memberId) ?: eventMembershipService.create(
                    EventMembership(
                        eventId = event.id,
                        userId = memberId,
                        role = MembershipRole.MEMBER,
                    ),
                )
            call.respond(HttpStatusCode.OK, membership)
        }
    }

    suspend fun getAdmins(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val event = eventService.getIfHasReadAccess(eventId = eventId, userId = userId)
        if (event == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val memberships = eventMembershipService.allByEvent(eventId = event.id)
            val users =
                memberships
                    .filter {
                        it.role == MembershipRole.ADMIN || it.role == MembershipRole.OWNER
                    }.map { userService.read(it.userId) }
            call.respond(HttpStatusCode.OK, users)
        }
    }

    suspend fun getAdminById(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val adminId = call.routeId("memberId")
        val event = eventService.getIfHasReadAccess(eventId = eventId, userId = userId)
        if (adminId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing admin ID")
        } else if (event == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = eventMembershipService.byEventAndUser(eventId = event.id, userId = adminId)
            if (membership == null || (membership.role != MembershipRole.ADMIN && membership.role != MembershipRole.OWNER)) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, membership)
            }
        }
    }

    suspend fun makeAdmin(call: ApplicationCall) {
        setRoleIfAdmin(call, MembershipRole.ADMIN)
    }

    private suspend fun setRoleIfAdmin(
        call: ApplicationCall,
        role: MembershipRole,
    ) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val memberId = call.routeId("memberId")
        val event = eventService.getIfCanAdmin(eventId = eventId, userId = userId)
        if (memberId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing member ID")
        } else if (event == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership =
                eventMembershipService.byEventAndUser(eventId = event.id, userId = memberId) ?: eventMembershipService.create(
                    EventMembership(
                        eventId = event.id,
                        userId = memberId,
                        role = role,
                    ),
                )
            if (membership.role == MembershipRole.OWNER) {
                call.respond(HttpStatusCode.MethodNotAllowed, "Cannot change role of owner")
            } else {
                val membershipAsAdmin = membership.copy(role = role)
                eventMembershipService.update(membership.id, memberId, membershipAsAdmin)
                call.respond(HttpStatusCode.OK, membershipAsAdmin)
            }
        }
    }

    suspend fun removeAsAdmin(call: ApplicationCall) {
        setRoleIfAdmin(call, MembershipRole.MEMBER)
    }

    suspend fun allByUser(call: RoutingCall) {
        val callerId = call.oauthUserId()
        val userId = call.routeId("userId")
        if (callerId == null || userId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (callerId == userId) {
            call.respond(HttpStatusCode.OK, eventMembershipService.allOwnedByUser(userId = userId))
        } else {
            val callees = eventMembershipService.allOwnedByUser(userId = callerId)
            val users = eventMembershipService.allOwnedByUser(userId = userId)
            call.respond(HttpStatusCode.OK, callees.filter { it.eventId in users.map { membership -> membership.eventId } })
        }
    }
}
