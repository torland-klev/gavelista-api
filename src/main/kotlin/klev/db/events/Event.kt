package klev.db.events

import klev.plugins.InstantSerializer
import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class Event(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val name: String,
    @Serializable(with = InstantSerializer::class) val created: Instant,
    @Serializable(with = InstantSerializer::class) val eventStart: Instant,
    @Serializable(with = InstantSerializer::class) val eventEnd: Instant,
    val visibility: EventVisibility,
    @Serializable(with = UUIDSerializer::class) val createdByUserId: UUID,
)
