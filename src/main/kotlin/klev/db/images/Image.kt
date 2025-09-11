package klev.db.images

import io.ktor.http.ContentType
import klev.plugins.ContentTypeSerializer
import klev.plugins.ExposedBlobSerializer
import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.util.UUID

@Serializable
data class Image(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    @Serializable(with = UUIDSerializer::class) val userId: UUID,
    @Serializable(with = ExposedBlobSerializer::class) val image: ExposedBlob,
    @Serializable(with = ContentTypeSerializer::class) val fileType: ContentType?,
)
