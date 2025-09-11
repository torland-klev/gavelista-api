package klev.db.images

import io.ktor.http.ContentType
import klev.db.CRUD
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ImageService(
    database: Database,
) : CRUD<Image>(database, Images) {
    override suspend fun readMap(input: ResultRow) =
        Image(
            id = input[Images.id].value,
            userId = input[Images.userId],
            image = input[Images.image],
            fileType = input[Images.fileType]?.let { ContentType.parse(it) },
        )

    override suspend fun publicPrivacyFilter(input: Image) = false

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: Image,
    ) {
        statement[Images.id] = obj.id
        statement[Images.userId] = obj.userId
        statement[Images.image] = obj.image
        statement[Images.fileType] = obj.fileType?.toString()
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: Image,
    ) {
        update[Images.userId] = obj.userId
        update[Images.image] = obj.image
        update[Images.fileType] = obj.fileType?.toString()
        update[Images.updated] = CurrentTimestamp
    }

    suspend fun read(
        id: UUID?,
        userId: UUID?,
    ) = if (id == null || userId == null) {
        null
    } else {
        dbQuery {
            Images
                .selectAll()
                .where { (Images.userId eq userId) and (Images.id eq id) }
                .map { readMap(it) }
                .singleOrNull()
        }
    }

    suspend fun delete(
        id: UUID?,
        userId: UUID?,
    ) = if (id == null || userId == null) {
        0
    } else {
        dbQuery {
            Images.deleteWhere { (Images.userId eq userId) and (Images.id eq id) }
        }
    }
}
