package klev.db.wishes

import klev.db.UserCRUD
import klev.db.groups.GroupService
import klev.db.groups.groupsToWishes.GroupToWish
import klev.db.groups.groupsToWishes.GroupsToWishesService
import klev.db.groups.memberships.GroupMembershipService
import klev.db.images.ImageService
import klev.db.wishes.Wishes.description
import klev.db.wishes.Wishes.img
import klev.db.wishes.Wishes.occasion
import klev.db.wishes.Wishes.status
import klev.db.wishes.Wishes.title
import klev.db.wishes.Wishes.updated
import klev.db.wishes.Wishes.url
import klev.db.wishes.Wishes.userId
import klev.db.wishes.Wishes.visibility
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.util.UUID

class WishesService(
    database: Database,
    private val groupsToWishesService: GroupsToWishesService,
    private val groupMembershipService: GroupMembershipService,
    private val groupService: GroupService,
    private val imageService: ImageService,
) : UserCRUD<Wish>(database, Wishes) {
    override fun createMap(
        statement: InsertStatement<Number>,
        obj: Wish,
    ) {
        statement[userId] = obj.userId
        statement[occasion] = obj.occasion
        statement[status] = Status.OPEN
        statement[description] = obj.description
        statement[url] = obj.url
        statement[img] = obj.img
        statement[visibility] = obj.visibility
        statement[title] = obj.title
    }

    override suspend fun readMap(input: ResultRow): Wish =
        Wish(
            id = input[Wishes.id].value,
            userId = input[Wishes.userId],
            description = input[description],
            url = input[url],
            occasion = input[occasion],
            status = input[status],
            img = input[img],
            visibility = input[visibility],
            title = input[title],
        )

    override fun updateMap(
        update: UpdateStatement,
        obj: Wish,
    ) {
        update[userId] = obj.userId
        update[occasion] = obj.occasion
        update[status] = obj.status
        update[description] = obj.description
        update[url] = obj.url
        update[img] = obj.img
        update[visibility] = obj.visibility
        update[updated] = CurrentTimestamp
        update[title] = obj.title
    }

    override suspend fun allOwnedByUser(userId: UUID?) = super.allOwnedByUser(userId).filterNot { it.status == Status.DELETED }

    override suspend fun publicPrivacyFilter(input: Wish) = input.visibility == WishVisibility.PUBLIC

    suspend fun update(
        id: UUID?,
        userId: UUID?,
        partial: PartialWish,
    ): Wish? {
        val existing = read(id, userId)
        return if (existing == null) {
            null
        } else {
            val occasion = partial.occasion?.let { Occasion.valueOf(it.uppercase()) }
            val status = partial.status?.let { Status.valueOf(it.uppercase()) }
            val visibility = partial.visibility?.let { WishVisibility.valueOf(it.uppercase()) }
            val wish =
                update(
                    id,
                    userId,
                    existing.copy(
                        occasion = occasion ?: existing.occasion,
                        status = status ?: existing.status,
                        url = partial.url ?: existing.url,
                        description = partial.description ?: existing.description,
                        img = partial.img ?: existing.img,
                        visibility = visibility ?: existing.visibility,
                        title = partial.title ?: existing.title,
                    ),
                )

            try {
                if (existing.img != null && wish?.img != existing.img) {
                    imageService.delete(userId = userId, id = UUID.fromString(existing.img))
                }

                val groupId = UUID.fromString(partial.groupId)
                val group = groupService.getIfHasReadAccess(groupId = groupId, userId = userId)
                if (group != null) {
                    groupsToWishesService.deleteAllForWish(wishId = wish!!.id)
                    groupsToWishesService.create(GroupToWish(groupId = groupId, wishId = wish.id))
                }
            } catch (e: IllegalArgumentException) {
                // Ignore
            } catch (e: NullPointerException) {
                // Ignore
            }

            return wish
        }
    }

    override suspend fun delete(
        id: UUID?,
        userId: UUID?,
    ): Int {
        val existing = read(id, userId)
        return if (existing == null) {
            0
        } else {
            update(id, userId, existing.copy(status = Status.DELETED))
            1
        }
    }

    suspend fun createByPartial(
        partial: PartialWish,
        userId: UUID,
    ): Wish {
        val wish =
            create(
                Wish(
                    userId = userId,
                    occasion = partial.occasion?.let { Occasion.valueOf(it.uppercase()) } ?: Occasion.NONE,
                    description = partial.description,
                    url = partial.url,
                    img = partial.img,
                    visibility =
                        partial.visibility?.let { WishVisibility.valueOf(it.uppercase()) }
                            ?: if (partial.groupId == null) WishVisibility.PRIVATE else WishVisibility.GROUP,
                    title = partial.title!!,
                ),
            )

        try {
            val groupId = UUID.fromString(partial.groupId)
            val group = groupService.getIfHasReadAccess(groupId = groupId, userId = userId)
            if (group != null) {
                groupsToWishesService.create(GroupToWish(groupId = groupId, wishId = wish.id))
            }
        } catch (e: IllegalArgumentException) {
            // Ignore
        } catch (e: NullPointerException) {
            // Ignore
        }

        return wish
    }

    suspend fun allUserHasGroupAccessTo(oauthUserId: UUID?): Collection<Wish> =
        if (oauthUserId == null) {
            emptySet()
        } else {
            val groupMemberships = groupMembershipService.allOwnedByUser(oauthUserId)
            groupMemberships.flatMap { membership ->
                groupsToWishesService.allByGroup(membership.groupId).mapNotNull {
                    val wish = read(it.wishId)
                    if (wish?.visibility == WishVisibility.GROUP) {
                        wish
                    } else {
                        null
                    }
                }
            }
        }

    suspend fun allByGroup(
        userId: UUID?,
        groupId: UUID,
    ): Collection<Wish> =
        if (userId == null) {
            emptySet()
        } else {
            val groupMembership = groupMembershipService.byGroupAndUser(groupId = groupId, userId = userId)
            if (groupMembership == null) {
                emptySet()
            } else {
                groupsToWishesService.allByGroup(groupMembership.groupId).mapNotNull { read(it.wishId) }.filterNot {
                    it.visibility ==
                        WishVisibility.PRIVATE &&
                        it.userId != userId
                }
            }
        }

    suspend fun allUserHasReadAccessTo(userId: UUID?) = allPublic() + allOwnedByUser(userId) + allUserHasGroupAccessTo(userId)
}
