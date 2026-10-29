package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.registrationRequestData
import tech.testsys.domain.model.user.RegistrationRequest
import tech.testsys.domain.model.user.RegistrationRequestData
import java.time.Instant

class RegistrationRequestBuilderTests :
    DomainEntityBuilderTests<RegistrationRequest, RegistrationRequestData, RegistrationRequestDataBuilder>(
        RegistrationRequestBuilder(),
        RegistrationRequestDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = listOf(
        registrationRequestData {
            email = "user@example.com"
            confirmationCode = "123456"
            expiresAt = Instant.ofEpochSecond(100)
            attemptsLeft = 5
        },
    )
}
