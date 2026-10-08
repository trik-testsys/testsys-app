package tech.testsys.web.app.error

import org.junit.jupiter.api.Assertions.assertThrows
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.error.operation

/** The exception a service throws for an operation that failed with [error]. */
@OptIn(InternalOperationsApi::class)
internal fun operationFailure(error: OperationError): OperationException =
    assertThrows(OperationException::class.java) { operation<Nothing, OperationError> { raise(error) }.getOrThrow() }
