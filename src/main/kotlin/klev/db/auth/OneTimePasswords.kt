package klev.db.auth

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OneTimePasswords : UUIDTable() {
    val email = varchar(name = "email", length = 127)
    val code = integer(name = "code")
    val validUntil = timestamp("validUntil")
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
