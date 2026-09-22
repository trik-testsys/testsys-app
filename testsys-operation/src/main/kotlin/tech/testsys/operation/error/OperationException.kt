package tech.testsys.operation.error

/**
 * Exception form of a failed [OperationResult]. Its [cause] holds the stack trace of the place the [error] was
 * raised at.
 *
 * @property error the error the operation failed with.
 * @since %CURRENT_VERSION%
 */
class OperationException @PublishedApi internal constructor(
    val error: OperationError,
    cause: Throwable,
) : RuntimeException(error.toString(), cause)
