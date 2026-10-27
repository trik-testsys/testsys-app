package tech.testsys.web.app.config

import org.springframework.core.env.Environment
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.util.isAccessToken

private const val MAX_SUPERVISOR_NAME_CODE_POINTS = 512

/**
 * Creates the single Supervisor or sets its configured name and access code before the application accepts requests.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class SupervisorBootstrap(
    private val supervisors: SupervisorRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val transactions: TransactionOperations,
    private val accessTokenHasher: AccessTokenHasher,
) {
    /**
     * Returns the single Supervisor after creating it or setting its configured name and access code.
     *
     * @since %CURRENT_VERSION%
     */
    fun ensure(environment: Environment): Supervisor {
        val name = environment.getRequiredProperty("testsys.operation.supervisor.name").trim()
        val rawAccessToken = environment.getRequiredProperty("testsys.operation.supervisor.access-token")
        require(name.isNotEmpty() && name.codePointCount(0, name.length) <= MAX_SUPERVISOR_NAME_CODE_POINTS) {
            "Supervisor name must contain 1..512 Unicode characters after trimming"
        }
        require(isAccessToken(rawAccessToken)) {
            "Supervisor access code must consist of four groups of four Latin letters or digits separated by '-'"
        }

        return transactions.execute { ensureInTransaction(name, rawAccessToken) }
    }

    private fun ensureInTransaction(name: String, rawAccessToken: String): Supervisor {
        check(participants.findByAccessToken(rawAccessToken) == null && observers.findByAccessToken(rawAccessToken) == null) {
            "Supervisor access code is already held by a user of another kind"
        }

        val supervisor = supervisors.findSingle() ?: return supervisors.save(
            supervisorData {
                accessToken(rawAccessToken, accessTokenHasher)
                this.name = name
            },
        )

        val accessTokenHash = accessTokenHasher.hash(rawAccessToken)
        if (supervisor.data.name == name && supervisor.data.accessTokenHash == accessTokenHash) return supervisor
        return supervisors.update(
            supervisor.withData {
                accessToken(rawAccessToken, accessTokenHasher)
                this.name = name
            },
        )
    }
}
