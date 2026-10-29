package tech.testsys.operation.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.operation.annotation.InternalOperationsApi
import java.time.Duration
import java.time.Instant
import java.util.Random

@OptIn(InternalOperationsApi::class)
class InviteCodeTests {

    @Nested
    inner class GenerateInviteCodeTests {

        @ParameterizedTest
        @ValueSource(longs = [0, 1, 42, 1_000_000])
        fun `should generate 12 characters of the invite-code alphabet`(seed: Long) {
            val code = generateInviteCode(random = Random(seed), previous = null)

            assertTrue(Regex("[a-hjkmnp-z2-9]{12}").matches(code), "unexpected code $code")
        }

        @Test
        fun `should generate a code different from the replaced one`() {
            val repeated = generateInviteCode(random = Random(7), previous = null)
            val previous = InviteCodeHash(value = repeated, algorithm = HashAlgorithm.Identity)

            val code = generateInviteCode(random = Random(7), previous = previous)

            assertNotEquals(repeated, code)
        }

        @Test
        fun `should keep the first code if it differs from the replaced one`() {
            val expected = generateInviteCode(random = Random(7), previous = null)
            val previous = InviteCodeHash(value = "other", algorithm = HashAlgorithm.Identity)

            val code = generateInviteCode(random = Random(7), previous = previous)

            assertEquals(expected, code)
        }
    }

    @Nested
    inner class NormalizeInviteCodeTests {

        @ParameterizedTest
        @CsvSource(
            "ABCDEFGHJKMN, abcdefghjkmn",
            "AbC dEf, abc def",
            "' abc ', ' abc '",
            "abc-123, abc-123",
        )
        fun `should lowercase letters with the root locale and keep other characters`(input: String, expected: String) {
            assertEquals(expected, normalizeInviteCode(input))
        }

        @Test
        fun `should lowercase the dotted capital I by root locale rules rather than Turkish ones`() {
            assertEquals("i̇", normalizeInviteCode("İ"))
        }
    }

    @Nested
    inner class InviteExpiresAtTests {

        @Test
        fun `should add the ttl and truncate the moment to microseconds`() {
            val now = Instant.parse("2026-01-01T00:00:00.123456789Z")

            val actual = inviteExpiresAt(now = now, ttl = Duration.ofDays(1))

            assertEquals(Instant.parse("2026-01-02T00:00:00.123456Z"), actual)
        }

        @ParameterizedTest
        @ValueSource(longs = [0, -1])
        fun `should fail if the ttl is not positive`(ttlSeconds: Long) {
            assertThrows(IllegalStateException::class.java) {
                inviteExpiresAt(now = Instant.EPOCH, ttl = Duration.ofSeconds(ttlSeconds))
            }
        }
    }
}
