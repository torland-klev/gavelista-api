package klev.db.users.google

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAppUser(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val accessToken: String,
)
