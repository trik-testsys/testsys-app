package tech.testsys.infra.database.api.service

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ServiceUserFilterTests {

    @Test
    fun `should reject an empty role condition`() {
        assertThrows(IllegalArgumentException::class.java) { ServiceUserFilter(roles = emptySet()) }
    }
}
