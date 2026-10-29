package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.emailChangeRequestData
import tech.testsys.domain.model.user.EmailChangeRequest
import tech.testsys.domain.model.user.EmailChangeRequestData
import java.time.Instant

class EmailChangeRequestBuilderTests :
    DomainEntityBuilderTests<EmailChangeRequest, EmailChangeRequestData, EmailChangeRequestDataBuilder>(
        EmailChangeRequestBuilder(),
        EmailChangeRequestDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = listOf(
        emailChangeRequestData {
            user(10)
            email = "new@example.com"
            confirmationCode = "12345678"
            expiresAt = Instant.ofEpochSecond(100)
            attemptsLeft = 5
        },
    )
}
