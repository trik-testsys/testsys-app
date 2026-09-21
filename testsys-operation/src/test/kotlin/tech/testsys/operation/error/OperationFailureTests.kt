package tech.testsys.operation.error

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.util.assertRaises
import kotlin.test.Test

@OptIn(InternalOperationsApi::class)
class OperationFailureTests {

    private fun failedNested(): OperationResult<Int, AttachStatementError> =
        operation<Int, AttachStatementError> { TaskAlreadyHasStatementError.raise() }

    @Nested
    inner class GetOrRaiseTests {

        @Test
        fun `should return value if result is successful`() {
            val result = operation<Int, AttachStatementError> { 1.asSuccess().getOrRaise() }

            Assertions.assertEquals(OperationResult.Success(1), result)
        }

        @Test
        fun `should raise error of nested operation in enclosing operation`() {
            assertRaises(TaskAlreadyHasStatementError) {
                operation<Int, AttachStatementError> { failedNested().getOrRaise() }
            }
        }

        @Test
        fun `should keep failure of nested operation as cause`() {
            val nested = failedNested()

            val result = operation { nested.getOrRaise() }

            val nestedError = Assertions.assertInstanceOf(OperationResult.Error::class.java, nested)
            val error = Assertions.assertInstanceOf(OperationResult.Error::class.java, result)
            Assertions.assertSame(nestedError.failure, error.failure.cause?.cause)
        }

        @Test
        fun `should raise transformed error in enclosing operation`() {
            assertRaises(MissedDeveloperRoleError) {
                operation<Int, CreateTaskError> { failedNested().getOrRaise { MissedDeveloperRoleError } }
            }
        }

        @Test
        fun `should keep failure of nested operation as cause when error is transformed`() {
            val nested = failedNested()

            val result = operation<Int, CreateTaskError> { nested.getOrRaise { MissedDeveloperRoleError } }

            val nestedError = Assertions.assertInstanceOf(OperationResult.Error::class.java, nested)
            val error = Assertions.assertInstanceOf(OperationResult.Error::class.java, result)
            Assertions.assertSame(nestedError.failure, error.failure.cause?.cause)
        }
    }
}
