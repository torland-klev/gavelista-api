package klev.db.groups.groupsToWishes

import klev.db.CRUD
import klev.db.groups.groupsToWishes.GroupsToWishes.groupId
import klev.db.groups.groupsToWishes.GroupsToWishes.wishId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

class GroupsToWishesService(
    database: Database,
) : CRUD<GroupToWish>(database, GroupsToWishes) {
    override suspend fun readMap(input: ResultRow) =
        GroupToWish(
            groupId = input[groupId],
            wishId = input[wishId],
        )

    override suspend fun publicPrivacyFilter(input: GroupToWish) = false

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: GroupToWish,
    ) {
        statement[groupId] = obj.groupId
        statement[wishId] = obj.wishId
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: GroupToWish,
    ) {
        update[groupId] = obj.groupId
        update[wishId] = obj.wishId
    }

    suspend fun allByGroup(groupId: UUID) =
        dbQuery {
            GroupsToWishes.select(GroupsToWishes.groupId eq groupId).map { readMap(it) }
        }

    suspend fun allByWish(wishId: UUID) =
        dbQuery {
            GroupsToWishes.select(GroupsToWishes.wishId eq wishId).map { readMap(it) }
        }

    suspend fun deleteAllForWish(wishId: UUID) =
        dbQuery {
            GroupsToWishes.deleteWhere { GroupsToWishes.wishId eq wishId }
        }
}
