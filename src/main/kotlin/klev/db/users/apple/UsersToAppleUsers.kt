package klev.db.users.apple

import klev.db.UserTable
import org.jetbrains.exposed.sql.ReferenceOption

object UsersToAppleUsers : UserTable() {
    val appleUserId = varchar("appleUserId", 64).references(AppleUsers.id, onDelete = ReferenceOption.CASCADE)
    val authToken = varchar("authToken", 256)
}
