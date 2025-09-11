package klev.db.memberships

import klev.db.UserCRUD
import klev.db.memberships.EventMemberships.eventId
import klev.db.memberships.EventMemberships.role
import klev.db.memberships.EventMemberships.updated
import klev.db.memberships.EventMemberships.userId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EventMembershipService(
    database: Database,
) : UserCRUD<EventMembership>(database, EventMemberships) {
    override suspend fun readMap(input: ResultRow) =
        EventMembership(
            id = input[EventMemberships.id].value,
            eventId = input[eventId],
            userId = input[userId],
            role = input[role],
        )

    override suspend fun publicPrivacyFilter(input: EventMembership): Boolean = false

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: EventMembership,
    ) {
        statement[eventId] = obj.eventId
        statement[userId] = obj.userId
        statement[role] = obj.role
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: EventMembership,
    ) {
        update[eventId] = obj.eventId
        update[userId] = obj.userId
        update[role] = obj.role
        update[updated] = CurrentTimestamp
    }

    suspend fun isMember(
        userId: UUID,
        eventId: UUID,
    ) = dbQuery {
        EventMemberships.selectAll().where { (EventMemberships.userId eq userId) and (EventMemberships.eventId eq eventId) }.count()
    } > 0

    suspend fun canAdmin(
        userId: UUID,
        eventId: UUID,
    ) = dbQuery {
        EventMemberships
            .selectAll()
            .where {
                (EventMemberships.userId eq userId) and (EventMemberships.eventId eq eventId) and
                    ((role eq MembershipRole.ADMIN) or (role eq MembershipRole.OWNER))
            }.count()
    } > 0

    suspend fun allByEvent(eventId: UUID) =
        dbQuery {
            EventMemberships.selectAll().where { EventMemberships.eventId eq eventId }.map { readMap(it) }
        }

    override suspend fun delete(id: UUID) =
        dbQuery {
            EventMemberships.deleteWhere { EventMemberships.id eq id }
        } > 0

    suspend fun byEventAndUser(
        eventId: UUID,
        userId: UUID,
    ) = dbQuery {
        EventMemberships
            .selectAll()
            .where { (EventMemberships.eventId eq eventId) and (EventMemberships.userId eq userId) }
            .map {
                readMap(
                    it,
                )
            }.singleOrNull()
    }
}
