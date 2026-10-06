package tech.testsys.domain.model.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccessTokenHashingTests {

    @ParameterizedTest
    @ValueSource(strings = ["token", "", "  token  ", "Token", "Код-доступа"])
    fun `should preserve the raw access token with Identity`(rawAccessToken: String) {
        val result = hashAccessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)

        assertEquals(AccessTokenHash(value = rawAccessToken, algorithm = HashAlgorithm.Identity), result)
    }
}
