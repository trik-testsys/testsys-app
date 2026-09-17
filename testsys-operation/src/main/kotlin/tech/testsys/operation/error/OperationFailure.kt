package tech.testsys.operation.error

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class OperationFailure @PublishedApi internal constructor(
    val error: OperationError,
    @PublishedApi internal val scope: Raise<*>,
) : RuntimeException(error.toString())      // stack trace is captured

class Raise<in E : OperationError> @PublishedApi internal constructor() {
    fun raise(error: E): Nothing = throw OperationFailure(error, this)
}

inline fun <T, E : OperationError> operation(block: Raise<E>.() -> T): OperationResult<T, E> {
    val scope = Raise<E>()
    return try {
        scope.block().asSuccess()
    } catch (e: OperationFailure) {
        if (e.scope !== scope) throw e
        @Suppress("UNCHECKED_CAST")
        OperationResult.Error(e.error as E, e)
    }
}

context(raise: Raise<E>)
fun <E : OperationError> E.raise(): Nothing {
    raise.raise(this)
}

@OptIn(ExperimentalContracts::class)
context(raise: Raise<E>)
inline fun <E : OperationError> ensure(condition: Boolean, error: () -> E) {
    contract {
        callsInPlace(error, InvocationKind.AT_MOST_ONCE)
        returns() implies condition
    }
    if (!condition) raise.raise(error())
}

@OptIn(ExperimentalContracts::class)
context(raise: Raise<E>)
fun <E : OperationError> ensure(condition: Boolean, error: E) {
    contract {
        returns() implies condition
    }
    if (!condition) raise.raise(error)
}
