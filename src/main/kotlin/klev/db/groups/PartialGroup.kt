package klev.db.groups

import kotlinx.serialization.Serializable

@Serializable
data class PartialGroup(
    val name: String? = null,
    val visibility: String? = null,
    val members: List<String>? = emptyList(),
) {
    fun errorMessages() =
        if (name == null) {
            "Group name is required"
        } else if (visibility == null) {
            "Group visibility is required"
        } else if (GroupVisibility.entries.none { it.name == visibility.uppercase() }) {
            "Group visibility $visibility not supported. Supported values are ${GroupVisibility.entries}"
        } else {
            null
        }
}
