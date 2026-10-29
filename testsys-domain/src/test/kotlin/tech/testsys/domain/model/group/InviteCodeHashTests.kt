package tech.testsys.domain.model.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.model.user.HashAlgorithm

class InviteCodeHashTests {

    @ParameterizedTest
    @ValueSource(strings = ["abcdefghjkmn", "", "  abcd  ", "ABCDEFGHJKMN", "Код-приглашение"])
    fun `should preserve the raw invite code without normalizing it with Identity`(rawInviteCode: String) {
        val result = InviteCodeHash.hashInviteCode(rawInviteCode, algorithm = HashAlgorithm.Identity)

        assertEquals(InviteCodeHash(value = rawInviteCode, algorithm = HashAlgorithm.Identity), result)
    }

    @Test
    fun `should reject HmacSha256 because invite codes are hashed only with Identity`() {
        assertThrows<IllegalArgumentException> { InviteCodeHash.hashInviteCode("abcdefghjkmn", algorithm = HashAlgorithm.HmacSha256) }
    }
}
