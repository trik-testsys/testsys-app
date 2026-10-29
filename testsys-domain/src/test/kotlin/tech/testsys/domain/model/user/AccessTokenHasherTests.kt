package tech.testsys.domain.model.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AccessTokenHasherTests {

    private val hasher = AccessTokenHasher(SECRET)

    @ParameterizedTest
    @CsvSource(
        "ABCD-1234-EFGH-5678, 6028117f2a4529c3e0c1adc59cb34ee24a0ba26d33cf7d7d0664c81ec06cbc4f",
        "Код-доступа, b75afd7c30853f7618db3c37ded0f69007a7ea14e1873cd437cc63d2ab8d4c2b",
        "'', 081247dc68bb7fafbf13220013a0ab71db8b628d679161f87b5e5bd9e19b1494",
    )
    fun `should hash the access token with HMAC-SHA-256 as lower-case hex`(rawAccessToken: String, expected: String) {
        val result = hasher.hash(rawAccessToken)

        assertEquals(AccessTokenHash(value = expected, algorithm = HashAlgorithm.HmacSha256), result)
    }

    @Test
    fun `should produce the same hash if the same access token is hashed again`() {
        val first = hasher.hash("ABCD-1234-EFGH-5678")

        val second = AccessTokenHasher(SECRET).hash("ABCD-1234-EFGH-5678")

        assertEquals(first, second)
    }

    @Test
    fun `should produce different hashes if the secrets differ`() {
        val other = AccessTokenHasher(SECRET.reversed())

        val result = other.hash("ABCD-1234-EFGH-5678")

        assertNotEquals(hasher.hash("ABCD-1234-EFGH-5678").value, result.value)
    }

    @Test
    fun `should reject a secret shorter than 64 characters`() {
        assertThrows<IllegalArgumentException> { AccessTokenHasher(SECRET.dropLast(1)) }
    }

    private companion object {
        const val SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
