package klev.db.memberships

import klev.db.UserTable
import klev.db.events.Events
import org.jetbrains.exposed.v1.core.ReferenceOption

object EventMemberships : UserTable() {
    val eventId = uuid("eventId").references(Events.id, onDelete = ReferenceOption.CASCADE)
    val role = enumerationByName<MembershipRole>("role", 15)

    init {
        index(true, eventId, userId)
    }
}
