package klev.db.groups.memberships

import klev.db.UserCRUD
import klev.db.groups.Groups.updated
import klev.db.groups.memberships.GroupMemberships.groupId
import klev.db.groups.memberships.GroupMemberships.role
import klev.db.groups.memberships.GroupMemberships.userId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.util.UUID

class GroupMembershipService(
    database: Database,
) : UserCRUD<GroupMembership>(database, GroupMemberships) {
    override suspend fun readMap(input: ResultRow) =
        GroupMembership(
            id = input[GroupMemberships.id].value,
            groupId = input[groupId],
            userId = input[userId],
            role = input[role],
        )

    override suspend fun publicPrivacyFilter(input: GroupMembership): Boolean = false

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: GroupMembership,
    ) {
        statement[groupId] = obj.groupId
        statement[userId] = obj.userId
        statement[role] = obj.role
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: GroupMembership,
    ) {
        update[groupId] = obj.groupId
        update[userId] = obj.userId
        update[role] = obj.role
        update[updated] = CurrentTimestamp
    }

    suspend fun isMember(
        userId: UUID,
        groupId: UUID,
    ) = dbQuery {
        GroupMemberships.selectAll().where { (GroupMemberships.userId eq userId) and (GroupMemberships.groupId eq groupId) }.count()
    } > 0

    suspend fun isOwner(
        userId: UUID,
        groupId: UUID,
    ) = dbQuery {
        GroupMemberships
            .selectAll()
            .where {
                (GroupMemberships.userId eq userId) and (GroupMemberships.groupId eq groupId) and
                    (role eq GroupMembershipRole.OWNER)
            }.count()
    } > 0

    suspend fun canAdmin(
        userId: UUID,
        groupId: UUID,
    ) = dbQuery {
        GroupMemberships
            .selectAll()
            .where {
                (GroupMemberships.userId eq userId) and (GroupMemberships.groupId eq groupId) and
                    ((role eq GroupMembershipRole.ADMIN) or (role eq GroupMembershipRole.OWNER))
            }.count()
    } > 0

    suspend fun allByGroup(groupId: UUID) =
        dbQuery {
            GroupMemberships.selectAll().where { GroupMemberships.groupId eq groupId }.map { readMap(it) }
        }

    override suspend fun delete(id: UUID) =
        dbQuery {
            GroupMemberships.deleteWhere { GroupMemberships.id eq id }
        } > 0

    suspend fun byGroupAndUser(
        groupId: UUID,
        userId: UUID,
    ) = dbQuery {
        GroupMemberships
            .selectAll()
            .where { (GroupMemberships.groupId eq groupId) and (GroupMemberships.userId eq userId) }
            .map {
                readMap(
                    it,
                )
            }.singleOrNull()
    }
}
