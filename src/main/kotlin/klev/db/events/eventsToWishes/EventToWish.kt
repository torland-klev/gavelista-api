package klev.db.events.eventsToWishes

import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EventToWish(
    @Serializable(with = UUIDSerializer::class) val wishId: UUID,
    @Serializable(with = UUIDSerializer::class) val eventId: UUID,
)
