package klev.db.users.google

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object GoogleUsers : Table() {
    val id = varchar("id", length = 63)
    val name = varchar("name", length = 63)
    val givenName = varchar("givenName", length = 63)
    val familyName = varchar("familyName", length = 63)
    val email = varchar("email", length = 127)
    val verifiedEmail = bool("verifiedEmail")
    val picture = varchar("picture", length = 255)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
