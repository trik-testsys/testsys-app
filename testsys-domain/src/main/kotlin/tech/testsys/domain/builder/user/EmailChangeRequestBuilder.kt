package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import tech.testsys.domain.model.user.EmailChangeRequestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Builder of [EmailChangeRequestData]. Required: [user], [email], [confirmationCode], [expiresAt], [attemptsLeft].
 *
 * @property user the user changing the e-mail address, or `null` if not set yet.
 * @property email the new e-mail address being confirmed, or `null` if not set yet.
 * @property confirmationCode the confirmation code, or `null` if not set yet.
 * @property expiresAt the moment the confirmation code expires, or `null` if not set yet.
 * @property attemptsLeft the number of remaining attempts, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class EmailChangeRequestDataBuilder : Builder<EmailChangeRequestData> {

    var user: MultipleRoleUserId? = null

    var email: String? = null

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

    override fun build(): EmailChangeRequestData = EmailChangeRequestData(
        user = requireField(user) { ::user }.lazify(),
        email = requireField(email) { ::email },
        confirmationCode = requireField(confirmationCode) { ::confirmationCode },
        expiresAt = requireField(expiresAt) { ::expiresAt },
        attemptsLeft = requireField(attemptsLeft) { ::attemptsLeft },
    )
}

/**
 * Builder of [EmailChangeRequest] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class EmailChangeRequestBuilder :
    DomainEntityWithDataBuilder<EmailChangeRequest, EmailChangeRequestData, EmailChangeRequestDataBuilder>() {

    override fun dataBuilder() = EmailChangeRequestDataBuilder()

    override fun build(): EmailChangeRequest = EmailChangeRequest(
        id = EmailChangeRequestId(requireField(id) { ::id }),
        createdAt = requireField(createdAt) { ::createdAt },
        data = requireField(data) { ::data },
    ).applyVersion(version)
}
