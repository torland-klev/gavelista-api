package klev.db

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

abstract class UserCRUD<T>(
    database: Database,
    private val table: UserTable,
) : CRUD<T>(database, table) {
    open suspend fun allOwnedByUser(userId: UUID?) =
        if (userId == null) {
            emptyList()
        } else {
            dbQuery {
                table.selectAll().where { table.userId eq userId }.map { readMap(it) }
            }
        }

    override suspend fun delete(id: UUID) = false

    override suspend fun update(
        id: UUID,
        obj: T,
    ) = null

    suspend fun read(
        id: UUID?,
        userId: UUID?,
    ) = if (id == null || userId == null) {
        null
    } else {
        dbQuery {
            table
                .selectAll()
                .where { (table.userId eq userId) and (table.id eq id) }
                .map { readMap(it) }
                .singleOrNull()
        }
    }

    open suspend fun delete(
        id: UUID?,
        userId: UUID?,
    ) = if (id == null || userId == null) {
        0
    } else {
        dbQuery {
            table.deleteWhere { (table.userId eq userId) and (table.id eq id) }
        }
    }

    suspend fun update(
        id: UUID?,
        userId: UUID?,
        obj: T,
    ) = if (id == null || userId == null) {
        null
    } else {
        dbQuery {
            table.update({ (table.userId eq userId) and (table.id eq id) }) { updateMap(it, obj) }
        }
        read(id, userId)
    }

    suspend fun deleteAll(userId: UUID?) =
        if (userId == null) {
            0
        } else {
            dbQuery {
                table.deleteWhere { table.userId eq userId }
            }
        }
}
