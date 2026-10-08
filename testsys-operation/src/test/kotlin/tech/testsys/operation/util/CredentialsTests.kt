package tech.testsys.operation.util

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.operation.annotation.InternalOperationsApi
import java.util.random.RandomGenerator
import kotlin.test.assertEquals

@OptIn(InternalOperationsApi::class)
class CredentialsTests {

    private val random = mockk<RandomGenerator>()

    @Nested
    inner class NextConfirmationCodeTests {

        @Test
        fun `should build the code from eight random decimal digits`() {
            every { random.nextInt(10) } returnsMany listOf(0, 9, 1, 8, 2, 7, 3, 6)

            val code = random.nextConfirmationCode()

            assertEquals("09182736", code)
        }
    }

    @Nested
    inner class NextAccessTokenTests {

        @Test
        fun `should group sixteen random letters and digits by four separated by hyphens`() {
            every { random.nextInt(62) } returnsMany listOf(0, 25, 26, 51, 52, 61, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

            val token = random.nextAccessToken()

            assertEquals("AZaz-09BC-DEFG-HIJK", token)
        }
    }
}
