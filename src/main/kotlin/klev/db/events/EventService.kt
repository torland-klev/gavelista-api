package klev.db.events

import klev.db.CRUD
import klev.db.events.Events.created
import klev.db.events.Events.eventEnd
import klev.db.events.Events.eventStart
import klev.db.events.Events.name
import klev.db.events.Events.updated
import klev.db.events.Events.userId
import klev.db.events.Events.visibility
import klev.db.groups.Groups.createdBy
import klev.db.memberships.EventMembership
import klev.db.memberships.EventMembershipService
import klev.db.memberships.MembershipRole
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EventService(
    database: Database,
    private val eventMembershipService: EventMembershipService,
) : CRUD<Event>(database, Events) {
    override suspend fun readMap(input: ResultRow): Event =
        Event(
            id = input[Events.id].value,
            name = input[name],
            createdByUserId = input[userId],
            visibility = input[visibility],
            created = input[created],
            eventStart = input[eventStart],
            eventEnd = input[eventEnd],
        )

    override suspend fun publicPrivacyFilter(input: Event) = input.visibility == EventVisibility.PUBLIC

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: Event,
    ) {
        statement[name] = obj.name
        statement[userId] = obj.createdByUserId
        statement[visibility] = obj.visibility
        statement[eventStart] = obj.eventStart
        statement[eventEnd] = obj.eventEnd
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: Event,
    ) {
        update[name] = obj.name
        update[userId] = obj.createdByUserId
        update[visibility] = obj.visibility
        update[eventStart] = obj.eventStart
        update[eventEnd] = obj.eventEnd
        update[updated] = CurrentTimestamp
    }

    override suspend fun create(obj: Event): Event {
        val event = super.create(obj)
        eventMembershipService.create(
            EventMembership(
                eventId = event.id,
                userId = event.createdByUserId,
                role = MembershipRole.OWNER,
            ),
        )
        return event
    }

    suspend fun getIfHasReadAccess(
        eventId: UUID?,
        userId: UUID?,
    ): Event? =
        if (eventId == null || userId == null) {
            null
        } else {
            val event = read(eventId)
            if (event?.visibility == EventVisibility.PUBLIC || eventMembershipService.isMember(userId, eventId)) {
                event
            } else {
                null
            }
        }

    suspend fun getIfCanAdmin(
        eventId: UUID?,
        userId: UUID?,
    ): Event? =
        if (eventId == null || userId == null) {
            null
        } else if (eventMembershipService.canAdmin(userId, eventId)) {
            read(eventId)
        } else {
            null
        }

    suspend fun getIfCanInvite(
        eventId: UUID?,
        userId: UUID?,
    ): Event? =
        if (eventId == null || userId == null) {
            null
        } else if (eventMembershipService.canAdmin(userId = userId, eventId = eventId)) {
            read(eventId)
        } else {
            null
        }

    suspend fun allUserIsMemberOf(userId: UUID?) =
        if (userId == null) {
            emptyList()
        } else {
            eventMembershipService.allOwnedByUser(userId).mapNotNull { membership ->
                read(membership.eventId)
            }
        }

    suspend fun allCreatedByUser(userId: UUID?) =
        if (userId == null) {
            emptyList()
        } else {
            dbQuery {
                Events.selectAll().where { createdBy eq userId }.map { readMap(it) }
            }
        }

    suspend fun allMembersUserIsConnectedTo(id: UUID?) = allUserIsMemberOf(id).memberIds()

    private suspend fun List<Event>.memberIds() =
        flatMap { group -> eventMembershipService.allByEvent(group.id).map { it.userId } }.distinct()
}
