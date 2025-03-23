package klev.db.groups.invitations

import klev.db.UserTable
import klev.db.groups.Groups
import klev.db.users.Users
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Invitations : UserTable() {
    val groupId = uuid("groupId").references(Groups.id, onDelete = ReferenceOption.CASCADE)
    val invitee = uuid("invitee").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val validUntil = timestamp("validUntil")
}
