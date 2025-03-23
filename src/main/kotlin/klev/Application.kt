package klev

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import klev.db.auth.EmailService
import klev.db.auth.OneTimePasswordService
import klev.db.groups.GroupService
import klev.db.groups.GroupsRoutes
import klev.db.groups.groupsToWishes.GroupsToWishesService
import klev.db.groups.invitations.InvitationService
import klev.db.groups.memberships.GroupMembershipRoutes
import klev.db.groups.memberships.GroupMembershipService
import klev.db.images.ImageRoutes
import klev.db.images.ImageService
import klev.db.users.UserRoutes
import klev.db.users.UserService
import klev.db.users.apple.AppleUserService
import klev.db.users.google.GoogleUserService
import klev.db.wishes.WishesRoutes
import klev.db.wishes.WishesService
import klev.plugins.configureHTTP
import klev.plugins.configureRouting
import klev.plugins.configureSecurity
import klev.plugins.configureSerialization
import org.jetbrains.exposed.sql.Database
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = env("PORT").toInt(), host = env("HOST"), module = Application::module)
        .start(wait = true)
}

fun env(key: String): String = System.getenv(key) ?: dotenv()[key]

val applicationHttpClient =
    HttpClient(CIO) {
        install(HttpTimeout)
        install(ContentNegotiation) {
            json()
        }
    }

private val database =
    Database.connect(
        url = env("DB_URL"),
        user = env("DB_USER"),
        driver = env("DB_DRIVER"),
        password = env("DB_PASSWORD"),
    )

private val images =
    Database.connect(
        url = env("IMG_DB_URL"),
        user = env("IMG_DB_USER"),
        driver = env("IMG_DB_DRIVER"),
        password = env("IMG_DB_PASSWORD"),
    )

private val appleUserService = AppleUserService(database = database)
private val googleUserService = GoogleUserService(database = database)
private val userService =
    UserService(
        database = database,
        appleUserService = appleUserService,
        httpClient = applicationHttpClient,
        googleUserService = googleUserService,
    )
private val imageService = ImageService(database = images)

private val groupsToWishesService = GroupsToWishesService(database = database)
private val groupMembershipService = GroupMembershipService(database = database)
private val groupService = GroupService(database = database, groupMembershipService = groupMembershipService, userService = userService)
private val mailService = EmailService(httpClient = applicationHttpClient, userService = userService, groupService = groupService)
private val otpService = OneTimePasswordService(database = database, emailService = mailService)
private val invitationService = InvitationService(database = database, groupMembershipService = groupMembershipService)
private val wishesService =
    WishesService(
        database = database,
        groupsToWishesService = groupsToWishesService,
        groupMembershipService = groupMembershipService,
        groupService = groupService,
        imageService = imageService,
    )

fun ApplicationCall.oauthUserId() =
    try {
        principal<UserIdPrincipal>()?.name?.let { UUID.fromString(it) }
    } catch (e: IllegalArgumentException) {
        null
    }

fun ApplicationCall.routeId(param: String = "id") =
    try {
        parameters[param]?.let { UUID.fromString(it) }
    } catch (e: IllegalArgumentException) {
        null
    }

fun Application.module() {
    configureSecurity(
        httpClient = applicationHttpClient,
        userService = userService,
        invitationService = invitationService,
        otpService = otpService,
    )
    configureHTTP()
    configureSerialization()
    configureRouting(
        groupsRoutes =
            GroupsRoutes(
                groupMembershipService = groupMembershipService,
                groupService = groupService,
                invitationService = invitationService,
                mailService = mailService,
                userService = userService,
                wishesService = wishesService,
            ),
        userRoutes = UserRoutes(userService = userService),
        wishesRoutes = WishesRoutes(groupsToWishesService = groupsToWishesService, wishesService = wishesService),
        groupMembershipRoutes =
            GroupMembershipRoutes(
                groupService = groupService,
                userService = userService,
                groupMembershipService = groupMembershipService,
            ),
        imageRoutes = ImageRoutes(imageService),
    )
}
