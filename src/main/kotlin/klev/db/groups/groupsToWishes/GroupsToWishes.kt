package klev.db.groups.groupsToWishes

import klev.db.groups.Groups
import klev.db.wishes.Wishes
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object GroupsToWishes : UUIDTable() {
    val groupId = uuid("groupId").references(Groups.id, onDelete = ReferenceOption.CASCADE)
    val wishId = uuid("wishId").references(Wishes.id, onDelete = ReferenceOption.CASCADE)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
