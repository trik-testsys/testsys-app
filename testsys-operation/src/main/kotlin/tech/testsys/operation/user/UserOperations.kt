package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.User
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.AuthenticateError
import tech.testsys.operation.error.InvalidAccessTokenError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation

/**
 * Operations available to any user of the system, whatever their roles.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class UserOperations(
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val participantRepository: ParticipantRepository,
    private val observerRepository: ObserverRepository,
    private val supervisorRepository: SupervisorRepository,
) {

    /**
     * Returns the user whose current access code equals [accessToken] exactly, without changing any user.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.authentication")
    fun authenticate(accessToken: String): OperationResult<User<*>, AuthenticateError> = operation<User<*>, AuthenticateError> {
        val user: User<*>? = multipleRoleUserRepository.findByAccessToken(accessToken)
            ?: participantRepository.findByAccessToken(accessToken)
            ?: observerRepository.findByAccessToken(accessToken)
            ?: supervisorRepository.findByAccessToken(accessToken)
        ensure(user != null, InvalidAccessTokenError)
        return user.asSuccess()
    }
}
