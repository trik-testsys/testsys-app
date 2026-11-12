package tech.testsys.infra.database.api.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository

/**
 * Service grant and revocation of the supervisor role through [MultipleRoleUserRepository], which keeps the role data
 * row and the memberships in the role together.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class DatabaseServiceSupervisorRole(
    private val userJpaEntityRepository: UserJpaEntityRepository,
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val communityRepository: CommunityRepository,
) : ServiceSupervisorRole {

    @Transactional
    override fun grant(id: ServiceUserId, publicCommunityId: CommunityId): SupervisorRoleChange {
        val user = changeable(id, publicCommunityId) ?: return refusalOf(id)
        if (user.data.roles.any { role -> role is Supervisor }) return SupervisorRoleChange.Changed(user)

        return SupervisorRoleChange.Changed(
            multipleRoleUserRepository.addCommunityMembership(user.id, publicCommunityId, CommunityRole.Supervisor),
        )
    }

    @Transactional
    override fun revoke(id: ServiceUserId, publicCommunityId: CommunityId): SupervisorRoleChange {
        val user = changeable(id, publicCommunityId) ?: return refusalOf(id)
        if (user.data.roles.none { role -> role is Supervisor }) return SupervisorRoleChange.Changed(user)

        val revoked = user.withData { roles { removeAll { role -> role is Supervisor } } }
        return SupervisorRoleChange.Changed(multipleRoleUserRepository.update(revoked))
    }

    /** Returns the user with non-fixed roles [id] unless it is missing or owns [publicCommunityId]. */
    private fun changeable(id: ServiceUserId, publicCommunityId: CommunityId): MultipleRoleUser? {
        val user = multipleRoleUserRepository.findById(MultipleRoleUserId(id.value)) ?: return null
        val publicCommunity = checkNotNull(communityRepository.findById(publicCommunityId)) {
            "Public community ${publicCommunityId.value} does not exist"
        }
        return user.takeIf { publicCommunity.data.owner.id != user.id }
    }

    private fun refusalOf(id: ServiceUserId): SupervisorRoleChange = if (userJpaEntityRepository.findByIdOrNull(id.value) == null) {
        SupervisorRoleChange.NotFound
    } else {
        SupervisorRoleChange.NotAllowed
    }
}
