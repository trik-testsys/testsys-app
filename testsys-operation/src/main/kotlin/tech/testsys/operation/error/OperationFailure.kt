package tech.testsys.operation.error

import tech.testsys.operation.annotation.InternalOperationsApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Control-flow exception that interrupts an [operation] with an [OperationError]. Its [cause] is the exception of
 * the nested operation the error was taken from, so the original stack trace is kept.
 *
 * @property error the error the operation is interrupted with.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
class OperationFailure @PublishedApi internal constructor(
    val error: OperationError,
    @PublishedApi internal val scope: Raise<*>,
    cause: Throwable? = null,
) : RuntimeException(error.toString(), cause)      // stack trace is captured

/**
 * Scope of a single [operation] call, able to interrupt it with an error.
 *
 * @param E the type of the errors the operation can be interrupted with.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
class Raise<in E : OperationError> @PublishedApi internal constructor() {
    /**
     * Interrupts the operation with [error]; [cause] is the exception of a nested operation the [error] originates
     * from, if any.
     *
     * @since %CURRENT_VERSION%
     */
    @Suppress("MemberNameEqualsClassName")
    fun raise(error: E, cause: Throwable? = null): Nothing = throw OperationFailure(error, this, cause)
}

/**
 * Runs [block] as an operation: returns its value as a success, or the error it was interrupted with.
 *
 * @param T the type of the successful value.
 * @param E the type of the errors the operation can be interrupted with.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
inline fun <T, E : OperationError> operation(block: Raise<E>.() -> T): OperationResult<T, E> {
    val scope = Raise<E>()
    return try {
        scope.block().asSuccess()
    } catch (e: OperationFailure) {
        if (e.scope !== scope) throw e
        @Suppress("UNCHECKED_CAST")
        OperationResult.Error(e.error as E, OperationException(e.error, e))
    }
}

/**
 * Interrupts the enclosing operation with this error.
 *
 * @param E the type of the errors of the enclosing operation.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
context(scope: Raise<E>)
fun <E : OperationError> E.raise(): Nothing = scope.raise(this)

/**
 * Returns the value of a successful result or interrupts the enclosing operation with the error of a failed one,
 * keeping the original failure as the cause.
 *
 * @param T the type of the successful value.
 * @param E the type of the errors of the enclosing operation.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
context(raise: Raise<E>)
fun <T, E : OperationError> OperationResult<T, E>.getOrRaise(): T = when (this) {
    is OperationResult.Success -> value
    is OperationResult.Error -> raise.raise(error, failure)
}

/**
 * Returns the value of a successful result or interrupts the enclosing operation with the error of a failed one
 * converted by [transform], keeping the original failure as the cause.
 *
 * @param T the type of the successful value.
 * @param N the type of the errors of the nested operation.
 * @param E the type of the errors of the enclosing operation.
 * @since %CURRENT_VERSION%
 */
@InternalOperationsApi
context(raise: Raise<E>)
inline fun <T, N : OperationError, E : OperationError> OperationResult<T, N>.getOrRaise(
    transform: (N) -> E,
): T = when (this) {
    is OperationResult.Success -> value
    is OperationResult.Error -> raise.raise(transform(error), failure)
}

/**
 * Interrupts the enclosing operation with the result of [error] if [condition] is false; [error] is called only
 * on failure.
 *
 * @param E the type of the errors of the enclosing operation.
 * @since %CURRENT_VERSION%
 */
@OptIn(ExperimentalContracts::class)
@InternalOperationsApi
context(raise: Raise<E>)
inline fun <E : OperationError> ensure(condition: Boolean, error: () -> E) {
    contract {
        callsInPlace(error, InvocationKind.AT_MOST_ONCE)
        returns() implies condition
    }
    if (!condition) raise.raise(error())
}

/**
 * Interrupts the enclosing operation with [error] if [condition] is false.
 *
 * @param E the type of the errors of the enclosing operation.
 * @since %CURRENT_VERSION%
 */
@OptIn(ExperimentalContracts::class)
@InternalOperationsApi
context(raise: Raise<E>)
fun <E : OperationError> ensure(condition: Boolean, error: E) {
    contract {
        returns() implies condition
    }
    if (!condition) raise.raise(error)
}
