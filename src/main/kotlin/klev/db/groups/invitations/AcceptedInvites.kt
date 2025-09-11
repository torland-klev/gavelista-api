package klev.db.groups.invitations

import klev.db.groups.memberships.GroupMemberships
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object AcceptedInvites : UUIDTable() {
    val inviteId = uuid("inviteId").references(Invitations.id, onDelete = ReferenceOption.RESTRICT)
    val membershipId = uuid("membershipId").references(GroupMemberships.id, onDelete = ReferenceOption.CASCADE)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
