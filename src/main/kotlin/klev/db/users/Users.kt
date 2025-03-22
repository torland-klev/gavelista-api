package klev.db.users

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : UUIDTable() {
    val firstName = varchar("firstName", length = 63).nullable()
    val lastName = varchar("lastName", length = 63).nullable()
    val email = varchar("email", length = 127)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
