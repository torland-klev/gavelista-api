package klev.db.groups.groupsToWishes

import klev.db.groups.Groups
import klev.db.wishes.Wishes
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object GroupsToWishes : UUIDTable() {
    val groupId = uuid("groupId").references(Groups.id, onDelete = ReferenceOption.CASCADE)
    val wishId = uuid("wishId").references(Wishes.id, onDelete = ReferenceOption.CASCADE)
    val created = timestamp("created").defaultExpression(CurrentTimestamp)
    val updated = timestamp("updated").defaultExpression(CurrentTimestamp)
}
