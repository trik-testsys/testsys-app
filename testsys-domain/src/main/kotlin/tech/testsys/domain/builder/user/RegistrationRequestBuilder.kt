package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import tech.testsys.domain.model.user.RegistrationRequestId
import java.time.Instant

/**
 * Builder of [RegistrationRequestData]. Required: [email], [confirmationCode], [expiresAt], [attemptsLeft].
 *
 * @property email the e-mail address being confirmed, or `null` if not set yet.
 * @property confirmationCode the confirmation code, or `null` if not set yet.
 * @property expiresAt the moment the confirmation code expires, or `null` if not set yet.
 * @property attemptsLeft the number of remaining attempts, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class RegistrationRequestDataBuilder : Builder<RegistrationRequestData> {

    var email: String? = null

    var confirmationCode: String? = null

    var expiresAt: Instant? = null

    var attemptsLeft: Int? = null

    override fun build(): RegistrationRequestData {
        val email = requireField(email) { ::email }
        val confirmationCode = requireField(confirmationCode) { ::confirmationCode }
        val expiresAt = requireField(expiresAt) { ::expiresAt }
        val attemptsLeft = requireField(attemptsLeft) { ::attemptsLeft }

        return RegistrationRequestData(
            email = email,
            confirmationCode = confirmationCode,
            expiresAt = expiresAt,
            attemptsLeft = attemptsLeft,
        )
    }
}

/**
 * Builder of [RegistrationRequest] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class RegistrationRequestBuilder :
    DomainEntityWithDataBuilder<RegistrationRequest, RegistrationRequestData, RegistrationRequestDataBuilder>() {

    override fun dataBuilder() = RegistrationRequestDataBuilder()

    override fun build(): RegistrationRequest {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return RegistrationRequest(
            id = RegistrationRequestId(id),
            createdAt = createdAt,
            data = data,
        ).applyVersion(version)
    }
}
