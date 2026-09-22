package tech.testsys.operation.util

import org.junit.jupiter.api.Assertions
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationResult

inline fun <E : OperationError> assertRaises(expected: E, operation: () -> OperationResult<*, E>) {
    when (val result = operation()) {
        is OperationResult.Error<*> -> Assertions.assertEquals(expected, result.error)
        is OperationResult.Success<*> -> Assertions.fail("Should raise $expected, got ${result.value}")
    }
}