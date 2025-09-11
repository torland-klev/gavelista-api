package klev.db.memberships

import java.util.UUID

interface Membership {
    val id: UUID
    val userId: UUID
    val role: MembershipRole
}
