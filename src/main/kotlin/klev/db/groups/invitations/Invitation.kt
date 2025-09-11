package klev.db.groups.invitations

import klev.env
import klev.plugins.InstantSerializer
import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Clock.System.now
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@Serializable
data class Invitation(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val invitee: UUID,
    @Serializable(with = UUIDSerializer::class) val invitedBy: UUID,
    @Serializable(with = UUIDSerializer::class) val groupId: UUID,
    @Serializable(with = InstantSerializer::class) val validUntil: Instant = now().plus(3.toDuration(DurationUnit.DAYS)),
) {
    fun inviteUrl() = "${env("PUBLIC_URL")}/confirmInvite/$id"
}
