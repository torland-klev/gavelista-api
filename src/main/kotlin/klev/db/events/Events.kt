package klev.db.events

import klev.db.UserTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Events : UserTable() {
    val name = varchar("name", 31)
    val visibility = enumerationByName<EventVisibility>("visibility", 15)
    val eventStart = timestamp("eventStart")
    val eventEnd = timestamp("eventEnd")
}
