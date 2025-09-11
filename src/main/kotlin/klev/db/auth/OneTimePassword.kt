package klev.db.auth

import klev.plugins.InstantSerializer
import klev.plugins.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Clock.System.now
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
@Serializable
data class OneTimePassword(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val email: String,
    val code: Int = generateCode(),
    @Serializable(with = InstantSerializer::class) val validUntil: Instant = now().plus(10.toDuration(DurationUnit.MINUTES)),
) {
    companion object {
        fun generateCode() =
            Random.nextInt(
                from = 100_000,
                until = 1_000_000,
            )
    }
}
