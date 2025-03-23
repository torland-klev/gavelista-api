package klev.db.users

import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String,
) {
    fun displayName() = "$firstName $lastName"
}
