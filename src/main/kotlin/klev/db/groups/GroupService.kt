package klev.db.groups

import klev.db.CRUD
import klev.db.groups.Groups.createdBy
import klev.db.groups.Groups.name
import klev.db.groups.Groups.updated
import klev.db.groups.Groups.visibility
import klev.db.groups.memberships.GroupMembership
import klev.db.groups.memberships.GroupMembershipRole
import klev.db.groups.memberships.GroupMembershipService
import klev.db.users.UserService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.util.UUID

class GroupService(
    database: Database,
    private val groupMembershipService: GroupMembershipService,
    private val userService: UserService,
) : CRUD<Group>(database, Groups) {
    override suspend fun readMap(input: ResultRow): Group {
        val userId = input[createdBy]
        val user = requireNotNull(userService.read(userId)) { "User $userId not found" }
        return Group(id = input[Groups.id].value, name = input[name], createdBy = user, visibility = input[visibility])
    }

    override suspend fun publicPrivacyFilter(input: Group) = input.visibility == GroupVisibility.PUBLIC

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: Group,
    ) {
        statement[name] = obj.name
        statement[createdBy] = obj.createdBy.id
        statement[visibility] = obj.visibility
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: Group,
    ) {
        update[name] = obj.name
        update[createdBy] = obj.createdBy.id
        update[visibility] = obj.visibility
        update[updated] = CurrentTimestamp
    }

    override suspend fun create(obj: Group): Group {
        val group = super.create(obj)
        groupMembershipService.create(
            GroupMembership(
                groupId = group.id,
                userId = group.createdBy.id,
                role = GroupMembershipRole.OWNER,
            ),
        )
        return group
    }

    suspend fun allCreatedByUser(userId: UUID?) =
        if (userId == null) {
            emptyList()
        } else {
            dbQuery {
                Groups.selectAll().where { createdBy eq userId }.map { readMap(it) }
            }
        }

    suspend fun getIfHasReadAccess(
        groupId: UUID?,
        userId: UUID?,
    ): Group? =
        if (groupId == null || userId == null) {
            null
        } else {
            val group = read(groupId)
            if (group?.visibility == GroupVisibility.PUBLIC || groupMembershipService.isMember(userId, groupId)) {
                group
            } else {
                null
            }
        }

    suspend fun getIfCanAdmin(
        groupId: UUID?,
        userId: UUID?,
    ): Group? =
        if (groupId == null || userId == null) {
            null
        } else if (groupMembershipService.canAdmin(userId, groupId)) {
            read(groupId)
        } else {
            null
        }

    suspend fun allUserIsMemberOf(userId: UUID?) =
        if (userId == null) {
            emptyList()
        } else {
            groupMembershipService.allOwnedByUser(userId).mapNotNull { membership ->
                read(membership.groupId)
            }
        }
}
