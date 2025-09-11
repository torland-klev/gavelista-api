package klev.db.images

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.utils.io.readRemaining
import klev.db.users.UserAndSession
import klev.oauthUserId
import klev.routeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

class ImageRoutes(
    private val imageService: ImageService,
) {
    suspend fun getById(call: ApplicationCall) {
        val image = imageService.read(call.routeId(), call.oauthUserId())
        if (image != null) {
            call.respondBytes(
                bytes = image.image.bytes,
                contentType = image.fileType,
                status = HttpStatusCode.OK,
            )
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    suspend fun post(call: ApplicationCall) {
        call.oauthUserId()?.let { userId ->
            var imageBlob: ExposedBlob? = null
            var contentType: ContentType? = null
            try {
                call.receiveMultipart().forEachPart { part ->
                    if (part is PartData.FileItem && part.name?.lowercase() == "image") {
                        contentType = part.contentType
                        val byteArray =
                            withContext(Dispatchers.IO) {
                                part.provider().readRemaining().readByteArray()
                            }
                        imageBlob = ExposedBlob(byteArray)
                        part.dispose()
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
            }

            imageBlob?.let {
                val created =
                    imageService.create(
                        Image(
                            userId = userId,
                            image = it,
                            fileType = contentType,
                        ),
                    )
                call.respond(HttpStatusCode.Created, created.id.toString())
            }

                ?: call.respond(HttpStatusCode.BadRequest, "No image found in the request")
        } ?: call.respond(HttpStatusCode.Unauthorized)
    }

    suspend fun deleteById(call: ApplicationCall) {
        val rowsDeleted = imageService.delete(call.routeId(), call.oauthUserId())
        if (rowsDeleted > 0) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    suspend fun getById(
        call: ApplicationCall,
        userSession: UserAndSession,
    ) {
        val image = imageService.read(call.routeId(), userSession.user.id)
        if (image != null) {
            call.respondBytes(
                bytes = image.image.bytes,
                contentType = image.fileType,
                status = HttpStatusCode.OK,
            )
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
