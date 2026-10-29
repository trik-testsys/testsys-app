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
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.infra.database.api.session.CabinetSignInUserId
import tech.testsys.infra.settings.api.strictBinder
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.util.isAccessToken
import tech.testsys.web.app.security.logEndedSupervisorSessions

private const val MAX_SUPERVISOR_NAME_CODE_POINTS = 512

/**
 * Creates the single Supervisor or sets its configured name and access code before the application accepts requests;
 * a changed access code ends the remembered sign-ins of the Supervisor, those as other users included.
 *
 * @since 1.0.0-beta.1
 */
@OptIn(InternalOperationsApi::class)
class SupervisorBootstrap(
    private val supervisors: SupervisorRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val transactions: TransactionOperations,
    private val accessTokenHasher: AccessTokenHasher,
    private val signIns: CabinetSignInStore,
) {
    /**
     * Returns the single Supervisor after creating it or setting its configured name and access code.
     *
     * @since 1.0.0-beta.1
     */
    fun ensure(environment: Environment): Supervisor {
        // Bound one by one: a failure report of the binder would show the value of the access code.
        val binder = strictBinder(environment)
        fun required(key: String): String = checkNotNull(binder.bind(key, String::class.java).orElse(null)) { "$key is required" }
        val name = required("testsys.operation.supervisor.name").trim()
        val rawAccessToken = required("testsys.operation.supervisor.access-token")
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
        val isAccessTokenChanged = supervisor.data.accessTokenHash != accessTokenHash
        if (supervisor.data.name == name && !isAccessTokenChanged) return supervisor

        // The previous access code no longer signs in, so neither do the sign-ins made with it. They are deleted in
        // a transaction committed before the new code, so a failure in between leaves the previous code without them.
        if (isAccessTokenChanged) logEndedSupervisorSessions(signIns.deleteAllOf(CabinetSignInUserId(supervisor.id.value)))
        return supervisors.update(
            supervisor.withData {
                accessToken(rawAccessToken, accessTokenHasher)
                this.name = name
            },
        )
    }
}
