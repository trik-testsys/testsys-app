package tech.testsys.operation.error

import tech.testsys.operation.annotation.InternalOperationsApi

/**
 * Result of an [operation]: either a [Success] with a value or an [Error] with an [OperationError].
 *
 * @param T the type of the successful value.
 * @param E the type of the errors the operation can fail with.
 * @since %CURRENT_VERSION%
 */
sealed interface OperationResult<out T, out E : OperationError> {
    /**
     * Successful [OperationResult].
     *
     * @param T the type of the value.
     * @property value the value the operation completed with.
     * @since %CURRENT_VERSION%
     */
    data class Success<T>(val value: T) : OperationResult<T, Nothing>

    /**
     * Failed [OperationResult].
     *
     * @param E the type of the error.
     * @property error the error the operation was interrupted with.
     * @property failure the exception form of the error; its cause holds the stack trace of the failure.
     * @since %CURRENT_VERSION%
     */
    data class Error<E : OperationError>(val error: E, val failure: OperationException) : OperationResult<Nothing, E>
}

/**
 * Wraps this value into a successful [OperationResult].
 *
 * @param T the type of the value.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
fun <T> T.asSuccess(): OperationResult<T, Nothing> = OperationResult.Success(this)

/**
 * Returns the value of a successful result or throws the [OperationException] of a failed one. Intended for code
 * outside operations; inside an operation use [getOrRaise].
 *
 * @param T the type of the successful value.
 * @since %CURRENT_VERSION%
 */
fun <T> OperationResult<T, *>.getOrThrow(): T = when (this) {
    is OperationResult.Success -> value
    is OperationResult.Error -> throw failure
}

/**
 * Returns the value of a successful result or the result of [onError] applied to the error of a failed one.
 *
 * @param T the type of the successful value.
 * @param E the type of the errors of the result.
 * @since %CURRENT_VERSION%
 */
inline fun <T, E : OperationError> OperationResult<T, E>.getOrElse(onError: (E) -> T): T = when (this) {
    is OperationResult.Success -> value
    is OperationResult.Error -> onError(error)
}
