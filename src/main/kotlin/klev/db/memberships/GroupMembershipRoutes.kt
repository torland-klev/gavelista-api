package klev.db.memberships

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import klev.db.groups.GroupService
import klev.db.users.UserService
import klev.oauthUserId
import klev.routeId

class GroupMembershipRoutes(
    private val groupService: GroupService,
    private val userService: UserService,
    private val groupMembershipService: GroupMembershipService,
) {
    suspend fun allByGroup(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val group = groupService.getIfHasReadAccess(groupId = groupId, userId = userId)
        if (group == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val memberships = groupMembershipService.allByGroup(groupId = group.id)
            val users = memberships.map { userService.read(it.userId) }
            call.respond(HttpStatusCode.OK, users)
        }
    }

    suspend fun deleteIfCanAdmin(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val memberId = call.routeId("memberId")
        val group = groupService.getIfCanAdmin(groupId = groupId, userId = userId)
        if (group == null || memberId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val deleteSuccesses =
                groupMembershipService
                    .allByGroup(groupId = group.id)
                    .filter {
                        it.userId == memberId && it.groupId == group.id && it.role != MembershipRole.OWNER
                    }.map { groupMembershipService.delete(it.id) }
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
        val groupId = call.routeId("groupId")
        val memberId = call.routeId("memberId")
        val group = groupService.getIfCanAdmin(groupId = groupId, userId = userId)
        if (group == null || memberId == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = groupMembershipService.byGroupAndUser(groupId = group.id, userId = memberId)
            if (membership == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, membership)
            }
        }
    }

    suspend fun addUserToGroup(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val memberId = call.routeId("memberId")
        val group = groupService.getIfCanAdmin(groupId = groupId, userId = userId)
        if (memberId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing member ID")
        } else if (group == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership =
                groupMembershipService.byGroupAndUser(groupId = group.id, userId = memberId) ?: groupMembershipService.create(
                    GroupMembership(
                        groupId = group.id,
                        userId = memberId,
                        role = MembershipRole.MEMBER,
                    ),
                )
            call.respond(HttpStatusCode.OK, membership)
        }
    }

    suspend fun getAdmins(call: ApplicationCall) {
        val userId = call.oauthUserId()
        val groupId = call.routeId("groupId")
        val group = groupService.getIfHasReadAccess(groupId = groupId, userId = userId)
        if (group == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val memberships = groupMembershipService.allByGroup(groupId = group.id)
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
        val groupId = call.routeId("groupId")
        val adminId = call.routeId("memberId")
        val group = groupService.getIfHasReadAccess(groupId = groupId, userId = userId)
        if (adminId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing admin ID")
        } else if (group == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership = groupMembershipService.byGroupAndUser(groupId = group.id, userId = adminId)
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
        val groupId = call.routeId("groupId")
        val memberId = call.routeId("memberId")
        val group = groupService.getIfCanAdmin(groupId = groupId, userId = userId)
        if (memberId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing member ID")
        } else if (group == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            val membership =
                groupMembershipService.byGroupAndUser(groupId = group.id, userId = memberId) ?: groupMembershipService.create(
                    GroupMembership(
                        groupId = group.id,
                        userId = memberId,
                        role = role,
                    ),
                )
            if (membership.role == MembershipRole.OWNER) {
                call.respond(HttpStatusCode.MethodNotAllowed, "Cannot change role of owner")
            } else {
                val membershipAsAdmin = membership.copy(role = role)
                groupMembershipService.update(membership.id, memberId, membershipAsAdmin)
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
            call.respond(HttpStatusCode.OK, groupMembershipService.allOwnedByUser(userId = userId))
        } else {
            val callees = groupMembershipService.allOwnedByUser(userId = callerId)
            val users = groupMembershipService.allOwnedByUser(userId = userId)
            call.respond(HttpStatusCode.OK, callees.filter { it.groupId in users.map { membership -> membership.groupId } })
        }
    }
}
