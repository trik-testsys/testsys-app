package tech.testsys.operation.error


sealed interface OperationResult<out T, out E : OperationError> {
    data class Success<T>(val value: T) : OperationResult<T, Nothing>
    data class Error<E : OperationError>(val error: E, val failure: OperationFailure) : OperationResult<Nothing, E>
}

fun <T> T.asSuccess(): OperationResult<T, Nothing> = OperationResult.Success(this)

fun <T> OperationResult<T, *>.getOrThrow(): T = when (this) {
    is OperationResult.Success -> value
    is OperationResult.Error -> throw failure
}

inline fun <T, E : OperationError> OperationResult<T, E>.getOrElse(onError: (E) -> T): T = when (this) {
    is OperationResult.Success -> value
    is OperationResult.Error -> onError(error)
}


