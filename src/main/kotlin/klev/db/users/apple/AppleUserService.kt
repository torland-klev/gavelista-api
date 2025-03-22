package klev.db.users.apple

import klev.db.users.google.GoogleUsers
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

class AppleUserService(
    private val database: Database,
) {
    init {
        transaction(database) {
            SchemaUtils.create(AppleUsers)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    private suspend fun read(id: String): AppleUser? =
        dbQuery {
            AppleUsers
                .selectAll()
                .where { AppleUsers.id eq id }
                .map {
                    AppleUser(
                        userIdentifier = id,
                        givenName = it[AppleUsers.givenName],
                        email = it[AppleUsers.email],
                        familyName = it[AppleUsers.familyName],
                        authorizationCode = it[AppleUsers.authorizationCode],
                        identityToken = it[AppleUsers.identityToken],
                    )
                }.singleOrNull()
        }

    private suspend fun update(user: AppleUser) =
        dbQuery {
            AppleUsers.update({ AppleUsers.id eq user.userIdentifier }) {
                it[givenName] = user.givenName
                it[familyName] = user.familyName
                it[email] = user.email
                it[authorizationCode] = user.authorizationCode
                it[identityToken] = user.identityToken
                it[GoogleUsers.updated] = CurrentTimestamp
            }
        }

    private suspend fun create(user: AppleUser) =
        dbQuery {
            dbQuery {
                AppleUsers.insert {
                    it[id] = user.userIdentifier
                    it[givenName] = user.givenName
                    it[familyName] = user.familyName
                    it[email] = user.email
                    it[authorizationCode] = user.authorizationCode
                    it[identityToken] = user.identityToken
                }[AppleUsers.id]
            }
        }

    suspend fun createOrUpdate(user: AppleUser): AppleUser {
        val existing = read(user.userIdentifier)
        return if (existing != null) {
            update(existing)
            read(user.userIdentifier)!!
        } else {
            read(create(user))!!
        }
    }

    suspend fun read(appleUserDTO: AppleUserDTO) =
        dbQuery {
            AppleUsers
                .selectAll()
                .where {
                    (AppleUsers.authorizationCode eq appleUserDTO.authorizationCode) and (AppleUsers.id eq appleUserDTO.userIdentifier)
                }.singleOrNull()
                ?.get(AppleUsers.id)
                ?.let {
                    read(
                        it,
                    )
                }
        }
}
