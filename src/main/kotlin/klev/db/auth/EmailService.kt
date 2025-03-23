package klev.db.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.http.path
import klev.db.groups.GroupService
import klev.db.groups.invitations.Invitation
import klev.db.users.UserService
import klev.env
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class EmailService(
    private val httpClient: HttpClient,
    private val userService: UserService,
    private val groupService: GroupService,
) {
    suspend fun sendOneTimePassword(
        email: String,
        code: Int,
    ) {
        val response =
            httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = env("EMAIL_API_HOST")
                    path("api/send")
                }
                headers {
                    contentType(ContentType.Application.Json)
                    bearerAuth(env("EMAIL_API_TOKEN"))
                }
                timeout {
                    requestTimeoutMillis = 20_000
                }
                setBody(
                    """{
                |"from": {"email":"mailtrap@swiftgift.no","name":"SwiftGift"},
                |"to":[{"email":"$email"}],
                |"template_uuid":"${env("EMAIL_API_TEMPLATE")}",
                |"template_variables": {"code":"$code"}
                |}
                    """.trimMargin(),
                )
            }

        require(response.status.isSuccess()) {
            "Failed to send email OTP. Received status code ${response.status}"
        }
    }

    suspend fun sendGroupInvite(invite: Invitation) {
        val invitee = requireNotNull(userService.read(invite.invitee))
        val invitedBy = requireNotNull(userService.read(invite.invitedBy))
        val response =
            httpClient.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = env("EMAIL_API_HOST")
                    path("api/send")
                }
                headers {
                    contentType(ContentType.Application.Json)
                    bearerAuth(env("EMAIL_API_TOKEN"))
                }
                timeout {
                    requestTimeoutMillis = 20_000
                }
                setBody(
                    """{
                |"from": {"email":"mailtrap@swiftgift.no","name":"SwiftGift"},
                |"to":[{"email":"${invitee.email}"}],
                |"template_uuid":"${env("GROUP_INVITE_API_TEMPLATE")}",
                |"template_variables": {"validUntil":"${invite.validUntil.toLocalDateTime(
                        TimeZone.UTC,
                    ).date}","invitedBy":"${invitedBy.displayName()}","url":"${invite.inviteUrl()}","groupName":"${groupService.read(
                        invite.groupId,
                    )?.name}"}
                |}
                    """.trimMargin(),
                )
            }

        require(response.status.isSuccess()) {
            "Failed to send group invite. Received status code ${response.status}"
        }
    }
}
