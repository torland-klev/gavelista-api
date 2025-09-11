package klev.db.users.apple

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object AppleUsers : Table() {
    val id = varchar("id", length = 63)
    val givenName = varchar("givenName", length = 63)
    val familyName = varchar("familyName", length = 63)
    val email = varchar("email", length = 127)
    val authorizationCode = varchar("authorizationCode", length = 127)
    val identityToken = varchar("identityToken", length = 1023)

    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}
