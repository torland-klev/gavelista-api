package klev.db.memberships

import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EventMembership(
    @Serializable(with = UUIDSerializer::class) override val id: UUID = UUID.randomUUID(),
    @Serializable(with = UUIDSerializer::class) val eventId: UUID,
    @Serializable(with = UUIDSerializer::class) override val userId: UUID,
    override val role: MembershipRole,
) : Membership
