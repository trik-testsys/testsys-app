package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.user.AccessRestorationRequest
import tech.testsys.domain.model.user.AccessRestorationRequestData
import tech.testsys.domain.model.user.AccessRestorationRequestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Builder of [AccessRestorationRequestData]. Required: [user], [confirmationCode], [expiresAt], [attemptsLeft].
 *
 * @property user the user restoring access, or `null` if not set yet.
 * @property confirmationCode the confirmation code, or `null` if not set yet.
 * @property expiresAt the moment the confirmation code expires, or `null` if not set yet.
 * @property attemptsLeft the number of remaining attempts, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class AccessRestorationRequestDataBuilder : Builder<AccessRestorationRequestData> {

    var user: MultipleRoleUserId? = null

    var confirmationCode: String? = null

    var expiresAt: Instant? = null

    var attemptsLeft: Int? = null

    /**
     * Sets [user] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun user(user: Long) {
        this.user = MultipleRoleUserId(user)
    }

    override fun build(): AccessRestorationRequestData = AccessRestorationRequestData(
        user = requireField(user) { ::user }.lazify(),
        confirmationCode = requireField(confirmationCode) { ::confirmationCode },
        expiresAt = requireField(expiresAt) { ::expiresAt },
        attemptsLeft = requireField(attemptsLeft) { ::attemptsLeft },
    )
}

/**
 * Builder of [AccessRestorationRequest] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class AccessRestorationRequestBuilder :
    DomainEntityWithDataBuilder<AccessRestorationRequest, AccessRestorationRequestData, AccessRestorationRequestDataBuilder>() {

    override fun dataBuilder() = AccessRestorationRequestDataBuilder()

    override fun build(): AccessRestorationRequest = AccessRestorationRequest(
        id = AccessRestorationRequestId(requireField(id) { ::id }),
        createdAt = requireField(createdAt) { ::createdAt },
        data = requireField(data) { ::data },
    ).applyVersion(version)
}
