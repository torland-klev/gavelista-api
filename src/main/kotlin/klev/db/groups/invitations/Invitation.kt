package klev.db.groups.invitations

import klev.env
import klev.plugins.UUIDSerializer
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class Invitation(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val invitee: UUID,
    @Serializable(with = UUIDSerializer::class) val invitedBy: UUID,
    @Serializable(with = UUIDSerializer::class) val groupId: UUID,
    val validUntil: Instant = now().plus(3.toDuration(DurationUnit.DAYS)),
) {
    fun inviteUrl() = "${env("PUBLIC_URL")}/confirmInvite/$id"
}
