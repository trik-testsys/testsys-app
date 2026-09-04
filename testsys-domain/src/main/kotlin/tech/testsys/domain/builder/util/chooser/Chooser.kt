package tech.testsys.domain.builder.util.chooser

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.requireField

/**
 * Base class of DSL single-choice selectors: subclasses expose named functions that record a choice of [T],
 * and [build] fails if no choice was made.
 *
 * @param T the type of the chosen value.
 * @since %CURRENT_VERSION%
 */
abstract class Chooser<T> : Builder<T> {

    protected var choice: Builder<T>? = null
        private set

    protected fun makeChoice(choice: Builder<T>) {
        this.choice = choice
    }

    override fun build(): T {
        val choice = requireField(choice) { ::choice }
        return choice.build()
    }
}
