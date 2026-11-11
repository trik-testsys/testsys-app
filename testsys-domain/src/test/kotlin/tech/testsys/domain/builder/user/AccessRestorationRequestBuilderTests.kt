package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.accessRestorationRequestData
import tech.testsys.domain.model.user.AccessRestorationRequest
import tech.testsys.domain.model.user.AccessRestorationRequestData
import java.time.Instant

class AccessRestorationRequestBuilderTests :
    DomainEntityBuilderTests<AccessRestorationRequest, AccessRestorationRequestData, AccessRestorationRequestDataBuilder>(
        AccessRestorationRequestBuilder(),
        AccessRestorationRequestDataBuilder(),
    ) {
    override fun buildDataWithAllFields() = listOf(
        accessRestorationRequestData {
            user(10)
            confirmationCode = "12345678"
            expiresAt = Instant.ofEpochSecond(100)
            attemptsLeft = 5
        },
    )
}
