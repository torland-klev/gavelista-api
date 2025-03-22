package klev.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

abstract class CRUD<T>(
    private val database: Database,
    private val table: UUIDTable,
) {
    init {
        transaction(database) {
            SchemaUtils.create(table)
        }
    }

    protected abstract suspend fun readMap(input: ResultRow): T

    protected abstract fun updateMap(
        update: UpdateStatement,
        obj: T,
    )

    protected abstract fun createMap(
        statement: InsertStatement<Number>,
        obj: T,
    )

    open suspend fun allPublic() =
        dbQuery {
            table.selectAll().map { readMap(it) }.filter { publicPrivacyFilter(it) }
        }

    abstract suspend fun publicPrivacyFilter(input: T): Boolean

    open suspend fun read(id: UUID?) =
        if (id == null) {
            null
        } else {
            dbQuery {
                table
                    .selectAll()
                    .where { table.id eq id }
                    .map { readMap(it) }
                    .singleOrNull()
            }
        }

    open suspend fun update(
        id: UUID,
        obj: T,
    ): T? {
        dbQuery {
            table.update({
                table.id eq id
            }) {
                updateMap(it, obj)
            }
        }
        return read(id)
    }

    open suspend fun create(obj: T) =
        read(
            dbQuery {
                table
                    .insert {
                        createMap(it, obj)
                    }[table.id]
                    .value
            },
        )!!

    open suspend fun delete(id: UUID) =
        dbQuery {
            table.deleteWhere { table.id eq id } > 0
        }

    protected suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
