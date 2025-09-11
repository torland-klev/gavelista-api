package klev.db.events.eventsToWishes

import klev.db.events.Events
import klev.db.wishes.Wishes
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object EventsToWishes : UUIDTable() {
    val eventId = uuid("eventId").references(Events.id, onDelete = ReferenceOption.CASCADE)
    val wishId = uuid("wishId").references(Wishes.id, onDelete = ReferenceOption.CASCADE)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
