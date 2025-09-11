package klev.db.groups.invitations

import klev.db.UserTable
import klev.db.groups.Groups
import klev.db.users.Users
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Invitations : UserTable() {
    val groupId = uuid("groupId").references(Groups.id, onDelete = ReferenceOption.CASCADE)
    val invitee = uuid("invitee").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val validUntil = timestamp("validUntil")
}
