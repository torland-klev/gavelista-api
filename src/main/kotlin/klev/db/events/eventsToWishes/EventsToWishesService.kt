package klev.db.events.eventsToWishes

import klev.db.CRUD
import klev.db.events.eventsToWishes.EventsToWishes.wishId
import klev.db.memberships.EventMemberships.eventId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

class EventsToWishesService(
    database: Database,
) : CRUD<EventToWish>(database, EventsToWishes) {
    override suspend fun readMap(input: ResultRow) =
        EventToWish(
            eventId = input[eventId],
            wishId = input[wishId],
        )

    override suspend fun publicPrivacyFilter(input: EventToWish) = false

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: EventToWish,
    ) {
        statement[eventId] = obj.eventId
        statement[wishId] = obj.wishId
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: EventToWish,
    ) {
        update[eventId] = obj.eventId
        update[wishId] = obj.wishId
    }

    suspend fun allByEvent(eventId: UUID) =
        dbQuery {
            EventsToWishes.select(EventsToWishes.eventId eq eventId).map { readMap(it) }
        }

    suspend fun allByWish(wishId: UUID) =
        dbQuery {
            EventsToWishes.select(EventsToWishes.wishId eq wishId).map { readMap(it) }
        }

    suspend fun deleteAllForWish(wishId: UUID) =
        dbQuery {
            EventsToWishes.deleteWhere { EventsToWishes.wishId eq wishId }
        }
}
