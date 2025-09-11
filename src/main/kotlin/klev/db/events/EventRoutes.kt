package klev.db.events

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import klev.db.auth.EmailService
import klev.db.groups.invitations.InvitationService
import klev.db.memberships.EventMembership
import klev.db.memberships.EventMembershipService
import klev.db.memberships.MembershipRole
import klev.db.users.UserService
import klev.db.wishes.WishesService
import klev.oauthUserId
import klev.routeId
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class EventRoutes(
    private val eventMembershipService: EventMembershipService,
    private val eventService: EventService,
    private val invitationService: InvitationService,
    private val mailService: EmailService,
    private val userService: UserService,
    private val wishesService: WishesService,
) {
    suspend fun all(call: ApplicationCall) {
        val events =
            eventService.allPublic() +
                eventService.allCreatedByUser(call.oauthUserId()) +
                eventService.allUserIsMemberOf(call.oauthUserId())
        call.respond(events.toSet())
    }

    suspend fun post(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val user = userService.read(userId)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val partialEvent = call.receive<PartialEvent>()
                val partialEventError = partialEvent.errorMessages()

                if (partialEventError != null) {
                    call.respond(HttpStatusCode.BadRequest, partialEventError)
                } else {
                    val createdEvent =
                        eventService.create(
                            Event(
                                name = partialEvent.name!!,
                                createdByUserId = userId,
                                visibility = EventVisibility.valueOf(partialEvent.visibility!!.uppercase()),
                                created = Clock.System.now(),
                                eventEnd = Instant.parse(partialEvent.eventEnd!!),
                                eventStart = Instant.parse(partialEvent.eventStart!!),
                            ),
                        )
                    partialEvent.members?.forEach { memberId ->
                        val invite =
                            invitationService.eventInvite(
                                eventId = createdEvent.id,
                                invitee = UUID.fromString(memberId),
                                invitedBy = user.id,
                            )

                        mailService.sendGroupInvite(invite)
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        createdEvent,
                    )
                }
            }
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }

    suspend fun get(call: ApplicationCall) {
        val eventId = call.routeId("eventId")
        val event = eventService.getIfHasReadAccess(userId = call.oauthUserId(), eventId = eventId)
        if (event == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, event)
        }
    }

    suspend fun deleteIfAdmin(call: ApplicationCall) {
        val eventId = call.routeId("eventId")
        val userId = call.oauthUserId()
        if (eventId == null || userId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (eventMembershipService.canAdmin(userId, eventId)) {
            if (eventService.delete(eventId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotModified)
            }
        } else {
            call.respond(HttpStatusCode.Unauthorized, "Only event owners can delete a event")
        }
    }

    suspend fun updateIfAdmin(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val user = userService.read(userId)
        if (user == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val partialEvent = call.receive<PartialEvent>()
            val partialEventError = partialEvent.errorMessages()
            val event = eventService.getIfCanAdmin(userId = userId, eventId = eventId)
            if (event == null) {
                call.respond(HttpStatusCode.NotFound)
            } else if (partialEventError != null) {
                call.respond(HttpStatusCode.BadRequest, partialEventError)
            } else {
                val newEvent =
                    event.copy(
                        name = partialEvent.name!!,
                        visibility = EventVisibility.valueOf(partialEvent.visibility!!.uppercase()),
                    )
                eventService.update(id = event.id, obj = newEvent)
                call.respond(HttpStatusCode.OK, newEvent)
            }
        }
    }

    suspend fun inviteIfCanInvite(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val user = userService.read(userId)
        if (user == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val event = eventService.getIfCanInvite(userId = userId, eventId = eventId)
            if (event == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val invite =
                    invitationService.eventInvite(
                        eventId = event.id,
                        invitedBy = user.id,
                        invitee = user.id,
                    )
                call.respond(HttpStatusCode.Created, invite.inviteUrl())
            }
        }
    }

    suspend fun allWishes(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val user = userService.read(userId)
        if (user == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, wishesService.allByEvent(userId = userId, eventId = eventId))
        }
    }

    suspend fun wishesForMember(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        val memberId = call.routeId("memberId")
        val user = userService.read(userId)
        if (user == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (!eventMembershipService.isMember(userId!!, eventId)) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.OK, wishesService.allByEvent(userId = memberId, eventId = eventId))
        }
    }

    suspend fun getRoleInEvent(call: RoutingCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        if (userId == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = eventMembershipService.byEventAndUser(eventId, userId)
            if (membership == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                call.respond(HttpStatusCode.OK, membership.role.name)
            }
        }
    }

    suspend fun isInEvent(call: RoutingCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        if (userId == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = eventMembershipService.byEventAndUser(eventId, userId)
            if (membership == null) {
                call.respond(HttpStatusCode.OK, false)
            } else {
                call.respond(HttpStatusCode.OK, true)
            }
        }
    }

    suspend fun joinEvent(call: RoutingCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        if (userId == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val event = eventService.getIfHasReadAccess(eventId, userId)
            val eventMembership = eventMembershipService.byEventAndUser(eventId, userId)
            if (eventMembership != null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else if (event != null) {
                val membership =
                    eventMembershipService.create(
                        EventMembership(
                            eventId = event.id,
                            userId = userId,
                            role = MembershipRole.MEMBER,
                        ),
                    )
                call.respond(HttpStatusCode.OK, membership)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

    suspend fun leaveEvent(call: RoutingCall) {
        val userId = call.oauthUserId()
        val eventId = call.routeId("eventId")
        if (userId == null || eventId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val event = eventService.getIfHasReadAccess(eventId, userId)
            val eventMembership = eventMembershipService.byEventAndUser(eventId, userId)
            if (eventMembership == null) {
                call.respond(HttpStatusCode.NotFound)
            } else if (event != null) {
                eventMembershipService.delete(eventMembership.id, userId)
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
