package tech.testsys.infra.crypto.api

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpringPasswordHasherTests {

    private val hasher = SpringPasswordHasher()

    @Nested
    inner class HashTests {

        @Test
        fun `should store bcrypt hashes with the algorithm prefix`() {
            val hash = hasher.hash("correct horse")

            assertTrue(hash.value.startsWith("{bcrypt}"))
        }

        @Test
        fun `should produce different hashes of the same password`() {
            val first = hasher.hash("correct horse")

            val second = hasher.hash("correct horse")

            assertNotEquals(first, second)
        }

        @Test
        fun `should hash a password of exactly 72 bytes in UTF-8`() {
            val password = "п".repeat(36)

            val hash = hasher.hash(password)

            assertTrue(hasher.matches(password, hash))
        }

        @Test
        fun `should refuse to hash a password longer than 72 bytes in UTF-8`() {
            assertThrows<IllegalArgumentException> { hasher.hash("a".repeat(73)) }
        }
    }

    @Nested
    inner class MatchesTests {

        private val hash = hasher.hash("correct horse")

        @Test
        fun `should match the password the hash was produced from`() {
            assertTrue(hasher.matches("correct horse", hash))
        }

        @Test
        fun `should not match another password`() {
            assertFalse(hasher.matches("Correct horse", hash))
        }

        @Test
        fun `should not match a password with surrounding spaces`() {
            assertFalse(hasher.matches(" correct horse ", hash))
        }

        @Test
        fun `should not match without a stored hash`() {
            assertFalse(hasher.matches("correct horse", null))
        }

        @Test
        fun `should not match a password longer than 72 bytes in UTF-8`() {
            val password = "a".repeat(73)

            assertFalse(hasher.matches(password, hash))
        }

        @Test
        fun `should not match a password whose first 72 bytes equal a stored password`() {
            val stored = hasher.hash("a".repeat(72))

            assertFalse(hasher.matches("a".repeat(72) + "b", stored))
        }
    }
}
