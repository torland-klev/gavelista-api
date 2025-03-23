package klev.db.groups

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import klev.db.auth.EmailService
import klev.db.groups.invitations.InvitationService
import klev.db.groups.memberships.GroupMembershipService
import klev.db.users.UserService
import klev.db.wishes.WishesService
import klev.oauthUserId
import klev.routeId
import java.util.UUID

class GroupsRoutes(
    private val groupMembershipService: GroupMembershipService,
    private val groupService: GroupService,
    private val invitationService: InvitationService,
    private val mailService: EmailService,
    private val userService: UserService,
    private val wishesService: WishesService,
) {
    suspend fun all(call: ApplicationCall) {
        val groups =
            groupService.allPublic() +
                groupService.allCreatedByUser(call.oauthUserId()) +
                groupService.allUserIsMemberOf(call.oauthUserId())
        call.respond(groups.toSet())
    }

    suspend fun post(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            val user = userService.read(userId)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                val partialGroup = call.receive<PartialGroup>()
                val partialGroupError = partialGroup.errorMessages()

                if (partialGroupError != null) {
                    call.respond(HttpStatusCode.BadRequest, partialGroupError)
                } else {
                    val createdGroup =
                        groupService.create(
                            Group(
                                name = partialGroup.name!!,
                                createdBy = user,
                                visibility = GroupVisibility.valueOf(partialGroup.visibility!!.uppercase()),
                            ),
                        )
                    partialGroup.members?.forEach { memberId ->
                        val invite =
                            invitationService.create(
                                groupId = createdGroup.id,
                                invitee = UUID.fromString(memberId),
                                invitedBy = user.id,
                            )

                        mailService.sendGroupInvite(invite)
                    }

                    call.respond(
                        HttpStatusCode.Created,
                        createdGroup,
                    )
                }
            }
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }

    suspend fun get(call: ApplicationCall) {
        val groupId = call.routeId("groupId")
        val group = groupService.getIfHasReadAccess(userId = call.oauthUserId(), groupId = groupId)
        if (group == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, group)
        }
    }

    suspend fun deleteIfOwner(call: ApplicationCall) {
        val groupId = call.routeId("groupId")
        val userId = call.oauthUserId()
        if (groupId == null || userId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (groupMembershipService.isOwner(userId, groupId)) {
            if (groupService.delete(groupId)) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotModified)
            }
        } else {
            call.respond(HttpStatusCode.Unauthorized, "Only group owners can delete a group")
        }
    }

    suspend fun updateIfAdmin(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val user = userService.read(userId)
        if (user == null || groupId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val partialGroup = call.receive<PartialGroup>()
            val partialGroupError = partialGroup.errorMessages()
            val group = groupService.getIfCanAdmin(userId = userId, groupId = groupId)
            if (group == null) {
                call.respond(HttpStatusCode.NotFound)
            } else if (partialGroupError != null) {
                call.respond(HttpStatusCode.BadRequest, partialGroupError)
            } else {
                val newGroup =
                    group.copy(
                        name = partialGroup.name!!,
                        visibility = GroupVisibility.valueOf(partialGroup.visibility!!.uppercase()),
                    )
                groupService.update(id = group.id, obj = newGroup)
                call.respond(HttpStatusCode.OK, newGroup)
            }
        }
    }

    suspend fun inviteIfAdmin(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val user = userService.read(userId)
        if (user == null || groupId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val group = groupService.getIfCanAdmin(userId = userId, groupId = groupId)
            if (group == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val invite =
                    invitationService.create(
                        groupId = group.id,
                        invitedBy = user.id,
                        invitee = user.id,
                    )
                call.respond(HttpStatusCode.Created, invite.inviteUrl())
            }
        }
    }

    suspend fun allWishes(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val user = userService.read(userId)
        if (user == null || groupId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, wishesService.allByGroup(userId = userId, groupId = groupId))
        }
    }

    suspend fun wishesForMember(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val memberId = call.routeId("memberId")
        val user = userService.read(userId)
        if (user == null || groupId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (!groupMembershipService.isMember(userId!!, groupId)) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            call.respond(HttpStatusCode.OK, wishesService.allByGroup(userId = memberId, groupId = groupId))
        }
    }

    suspend fun getRoleInGroup(call: RoutingCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        if (userId == null || groupId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = groupMembershipService.byGroupAndUser(groupId, userId)
            if (membership == null) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                call.respond(HttpStatusCode.OK, membership.role.name)
            }
        }
    }
}
