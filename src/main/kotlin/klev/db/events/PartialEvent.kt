package klev.db.events

import kotlinx.serialization.Serializable

@Serializable
data class PartialEvent(
    val name: String? = null,
    val visibility: String? = null,
    val members: List<String>? = emptyList(),
    val eventStart: String? = null,
    val eventEnd: String? = null,
) {
    fun errorMessages() =
        if (name == null) {
            "Event name is required"
        } else if (visibility == null) {
            "Event visibility is required"
        } else if (EventVisibility.entries.none { it.name.equals(visibility, ignoreCase = true) }) {
            "Event visibility $visibility not supported. Supported values are ${EventVisibility.entries}"
        } else if (eventStart == null || eventEnd == null) {
            "Event start and end is required"
        } else {
            null
        }
}
