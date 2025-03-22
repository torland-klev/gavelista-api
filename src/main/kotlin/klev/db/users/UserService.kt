package klev.db.users

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.server.auth.BearerTokenCredential
import klev.db.users.apple.AppleUser
import klev.db.users.apple.AppleUserDTO
import klev.db.users.apple.AppleUserService
import klev.db.users.apple.UsersToAppleUsers
import klev.db.users.google.GoogleAppUser
import klev.db.users.google.GoogleUser
import klev.db.users.google.GoogleUserService
import klev.db.users.google.UsersToEmailUsers
import klev.db.users.google.UsersToGoogleUsers
import klev.env
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.Date
import java.util.UUID

class UserService(
    private val database: Database,
    private val appleUserService: AppleUserService,
    private val googleUserService: GoogleUserService,
    private val httpClient: HttpClient,
) {
    init {
        transaction(database) {
            SchemaUtils.create(Users, UsersToGoogleUsers, UsersToAppleUsers, UsersToEmailUsers)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    private suspend fun create(user: User): UUID =
        dbQuery {
            Users
                .insert {
                    it[firstName] = user.firstName
                    it[lastName] = user.lastName
                    it[email] = user.email
                }[Users.id]
                .value
        }

    suspend fun read(id: UUID?): User? =
        if (id == null) {
            null
        } else {
            dbQuery {
                Users
                    .selectAll()
                    .where { Users.id eq id }
                    .map { User(id = id, firstName = it[Users.firstName], lastName = it[Users.lastName], email = it[Users.email]) }
                    .singleOrNull()
            }
        }

    private suspend fun getUserFromGoogleUser(googleUser: GoogleUser): User? {
        val userId =
            dbQuery {
                UsersToGoogleUsers
                    .selectAll()
                    .where {
                        UsersToGoogleUsers.googleUserId eq googleUser.id
                    }.singleOrNull()
                    ?.getOrNull(UsersToGoogleUsers.userId)
            }
        return if (userId != null) {
            read(
                dbQuery {
                    userId
                },
            )
        } else {
            null
        }
    }

    private suspend fun getUserFromAppleUser(appleUser: AppleUser): User? {
        val userId =
            dbQuery {
                UsersToAppleUsers
                    .selectAll()
                    .where {
                        UsersToAppleUsers.appleUserId eq appleUser.userIdentifier
                    }.singleOrNull()
                    ?.getOrNull(UsersToAppleUsers.userId)
            }
        return if (userId != null) {
            read(
                dbQuery {
                    userId
                },
            )
        } else {
            null
        }
    }

    private suspend fun GoogleUser.toUser(token: String) =
        getUserFromGoogleUser(this)?.also {
            updateAuthToken(this, token)
        } ?: read(
            create(
                User(
                    firstName = givenName,
                    lastName = familyName,
                    email = email,
                ),
            ).also { uid ->
                dbQuery {
                    UsersToGoogleUsers.insert {
                        it[userId] = uid
                        it[googleUserId] = this@toUser.id
                        it[authToken] = token
                    }
                }
            },
        )!!

    private suspend fun AppleUser.toUser() =
        getUserFromAppleUser(this) ?: read(
            create(
                User(firstName = this.givenName, lastName = this.familyName, email = this.email),
            ).also { uid ->
                dbQuery {
                    UsersToAppleUsers.insert {
                        it[userId] = uid
                        it[appleUserId] = this@toUser.userIdentifier
                        it[authToken] = this@toUser.identityToken
                    }
                }
            },
        )!!

    private suspend fun updateAuthToken(
        googleUser: GoogleUser,
        token: String,
    ) {
        dbQuery {
            UsersToGoogleUsers.update({ UsersToGoogleUsers.googleUserId eq googleUser.id }) {
                it[authToken] = token
                it[updated] = CurrentTimestamp
            }
        }
    }

    suspend fun createOrUpdate(session: UserSession) =
        googleUserService.createOrUpdate(googleUser = GoogleUser.fromSession(httpClient, session.token)).toUser(session.token)

    suspend fun getUserByToken(tokenCredential: BearerTokenCredential) =
        dbQuery {
            UsersToGoogleUsers
                .selectAll()
                .where { UsersToGoogleUsers.authToken eq tokenCredential.token }
                .singleOrNull()
                ?.get(UsersToGoogleUsers.userId)
        }?.let { read(it) } ?: dbQuery {
            UsersToEmailUsers
                .selectAll()
                .where { UsersToEmailUsers.authToken eq tokenCredential.token }
                .singleOrNull()
                ?.get(UsersToEmailUsers.userId)
        }?.let { read(it) }

    suspend fun createOrUpdate(appleUser: AppleUser) = appleUserService.createOrUpdate(appleUser).toUser()

    suspend fun createOrUpdate(googleAppUser: GoogleAppUser) =
        googleUserService
            .createOrUpdate(
                googleUser = GoogleUser.fromSession(httpClient, googleAppUser.accessToken),
            ).toUser(googleAppUser.accessToken)

    suspend fun read(appleUserDTO: AppleUserDTO) = appleUserService.read(appleUserDTO = appleUserDTO)?.toUser()

    suspend fun getUserByEmail(email: String?): User? =
        if (email == null) {
            null
        } else {
            dbQuery {
                Users
                    .selectAll()
                    .where { Users.email eq email }
                    .map {
                        User(
                            id = it[Users.id].value,
                            firstName = it[Users.firstName],
                            lastName = it[Users.lastName],
                            email = it[Users.email],
                        )
                    }.singleOrNull()
            }
        }

    suspend fun getAuthTokenIfExistingUser(user: User) =
        dbQuery {
            UsersToGoogleUsers
                .selectAll()
                .where { UsersToGoogleUsers.userId eq user.id }
                .singleOrNull()
                ?.get(UsersToGoogleUsers.authToken)
        } ?: dbQuery {
            UsersToEmailUsers
                .selectAll()
                .where { UsersToEmailUsers.userId eq user.id }
                .singleOrNull()
                ?.get(UsersToEmailUsers.authToken)
        }

    private fun generateAuthToken(email: String) =
        JWT
            .create()
            .withIssuer(env("APP_PUBLIC_NAME"))
            .withSubject(email)
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(env("APP_SECRET")))

    suspend fun createEmailUser(email: String): User {
        val newUserId = create(User(email = email))
        val newUser = requireNotNull(read(newUserId))
        dbQuery {
            UsersToEmailUsers.insert {
                it[userId] = newUser.id
                it[authToken] = generateAuthToken(email)
            }
        }
        return newUser
    }

    suspend fun update(
        id: UUID,
        partial: PartialUser,
    ) = read(id)?.let { user ->
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[firstName] = partial.firstName ?: user.firstName
                it[lastName] = partial.lastName ?: user.lastName
                it[email] = partial.email ?: user.email
                it[updated] = CurrentTimestamp
            }
        }
    } ?: 0

    suspend fun allPublic() =
        dbQuery {
            Users.selectAll().where { (Users.firstName neq null) and (Users.lastName neq null) }.map {
                User(
                    id = it[Users.id].value,
                    firstName = it[Users.firstName],
                    lastName = it[Users.lastName],
                    email = it[Users.email],
                )
            }
        }
}
