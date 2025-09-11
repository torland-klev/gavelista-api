package klev.db.wishes

import kotlinx.serialization.Serializable

@Serializable
data class PartialWish(
    val occasion: String? = null,
    val url: String? = null,
    val description: String? = null,
    val status: String? = null,
    val img: String? = null,
    val visibility: String? = null,
    val groupId: String? = null,
    val eventId: String? = null,
    val title: String? = null,
)
