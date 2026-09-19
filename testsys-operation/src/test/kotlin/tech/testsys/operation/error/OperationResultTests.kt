package tech.testsys.operation.error

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import tech.testsys.operation.annotation.InternalOperationsApi
import kotlin.test.Test

@OptIn(InternalOperationsApi::class)
class OperationResultTests {

    private fun failedNested(): OperationResult<Int, AttachStatementError> =
        operation<Int, AttachStatementError> { TaskAlreadyHasStatementError.raise() }

    @Nested
    inner class GetOrThrowTests {

        @Test
        fun `should return value if result is successful`() {
            val result = 1.asSuccess().getOrThrow()

            Assertions.assertEquals(1, result)
        }

        @Test
        fun `should throw OperationException with error of failed result`() {
            val exception = Assertions.assertThrows(OperationException::class.java) { failedNested().getOrThrow() }

            Assertions.assertEquals(TaskAlreadyHasStatementError, exception.error)
        }

        @Test
        fun `should throw OperationException out of enclosing operation`() {
            Assertions.assertThrows(OperationException::class.java) {
                operation<Int, CreateTaskError> { failedNested().getOrThrow() }
            }
        }

        @Test
        fun `should keep failure that interrupted operation as cause`() {
            val exception = Assertions.assertThrows(OperationException::class.java) { failedNested().getOrThrow() }

            val cause = Assertions.assertInstanceOf(OperationFailure::class.java, exception.cause)
            Assertions.assertEquals(TaskAlreadyHasStatementError, cause.error)
        }
    }
}
