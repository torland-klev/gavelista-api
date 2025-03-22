package klev.db.groups

import klev.db.users.Users
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Groups : UUIDTable() {
    val name = varchar("name", 31)
    val createdBy = uuid("createdBy").references(Users.id)
    val visibility = enumerationByName<GroupVisibility>("visibility", 15)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
