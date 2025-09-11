package klev.db.groups.invitations

import klev.db.UserCRUD
import klev.db.groups.invitations.Invitations.invitee
import klev.db.groups.invitations.Invitations.validUntil
import klev.db.groups.memberships.GroupMembership
import klev.db.groups.memberships.GroupMembershipRole
import klev.db.groups.memberships.GroupMembershipService
import klev.db.groups.memberships.GroupMemberships.groupId
import klev.db.groups.memberships.GroupMemberships.userId
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class InvitationService(
    database: Database,
    private val groupMembershipService: GroupMembershipService,
) : UserCRUD<Invitation>(database, Invitations) {
    init {
        transaction(database) {
            SchemaUtils.create(AcceptedInvites)
        }
    }

    override suspend fun readMap(input: ResultRow) =
        Invitation(
            id = input[Invitations.id].value,
            groupId = input[Invitations.groupId],
            invitedBy = input[Invitations.userId],
            invitee = input[invitee],
            validUntil = input[validUntil],
        )

    override suspend fun publicPrivacyFilter(input: Invitation) = false

    override fun createMap(
        statement: InsertStatement<Number>,
        obj: Invitation,
    ) {
        statement[groupId] = obj.groupId
        statement[userId] = obj.invitedBy
        statement[validUntil] = obj.validUntil
        statement[invitee] = obj.invitee
    }

    override fun updateMap(
        update: UpdateStatement,
        obj: Invitation,
    ) = Unit

    suspend fun create(
        groupId: UUID,
        invitee: UUID,
        invitedBy: UUID,
    ) = create(Invitation(id = UUID.randomUUID(), groupId = groupId, invitedBy = invitedBy, invitee = invitee))

    suspend fun completeInvitation(
        inviteId: UUID,
        invitedUser: UUID,
    ) {
        read(inviteId)?.let { invite ->
            val isMember = groupMembershipService.isMember(invitedUser, invite.groupId)
            if (!isMember) {
                val membership =
                    groupMembershipService.create(
                        GroupMembership(
                            groupId = invite.groupId,
                            userId = invitedUser,
                            role = GroupMembershipRole.MEMBER,
                        ),
                    )
                dbQuery {
                    AcceptedInvites.insert {
                        it[AcceptedInvites.inviteId] = invite.id
                        it[membershipId] = membership.id
                    }
                }
            }
        }
    }
}
