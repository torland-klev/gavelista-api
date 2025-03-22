package klev.db.users.google

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class GoogleUserService(
    private val database: Database,
) {
    init {
        transaction(database) {
            SchemaUtils.create(GoogleUsers)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO, database) { block() }

    private suspend fun read(id: String): GoogleUser? =
        dbQuery {
            GoogleUsers
                .selectAll()
                .where { GoogleUsers.id eq id }
                .map {
                    GoogleUser(
                        id = id,
                        name = it[GoogleUsers.name],
                        email = it[GoogleUsers.email],
                        verifiedEmail = it[GoogleUsers.verifiedEmail],
                        givenName = it[GoogleUsers.givenName],
                        familyName = it[GoogleUsers.familyName],
                        picture = it[GoogleUsers.picture],
                    )
                }.singleOrNull()
        }

    private suspend fun update(user: GoogleUser) {
        dbQuery {
            GoogleUsers.update({ GoogleUsers.id eq user.id }) {
                it[name] = user.name
                it[givenName] = user.givenName
                it[familyName] = user.familyName
                it[email] = user.email
                it[verifiedEmail] = user.verifiedEmail
                it[picture] = user.picture
                it[updated] = CurrentTimestamp
            }
        }
    }

    private suspend fun create(user: GoogleUser): String =
        dbQuery {
            GoogleUsers.insert {
                it[id] = user.id
                it[name] = user.name
                it[verifiedEmail] = user.verifiedEmail
                it[email] = user.email
                it[givenName] = user.givenName
                it[familyName] = user.familyName
                it[picture] = user.picture
            }[GoogleUsers.id]
        }

    suspend fun createOrUpdate(googleUser: GoogleUser): GoogleUser {
        val existing = read(googleUser.id)
        return if (existing != null) {
            update(existing)
            read(googleUser.id)!!
        } else {
            read(create(googleUser))!!
        }
    }
}
