package klev.plugins

import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.encodeBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            },
        )
    }
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) {
        encoder.encodeString(value.toString())
    }
}

object ExposedBlobSerializer : KSerializer<ExposedBlob> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ExposedBlob", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ExposedBlob,
    ) {
        encoder.encodeString(value.bytes.encodeBase64())
    }

    override fun deserialize(decoder: Decoder): ExposedBlob {
        val bytes = decoder.decodeString().decodeBase64Bytes()
        return ExposedBlob(bytes)
    }
}

object ContentTypeSerializer : KSerializer<ContentType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ContentType", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ContentType,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ContentType = ContentType.parse(decoder.decodeString())
}

@OptIn(ExperimentalTime::class)
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeString(value.toString())
    }
}
