package tech.testsys.domain.builder.util.chooser

/**
 * Abstract base class for DSL-style single-choice selectors.
 * Subclasses expose named functions that set the [choice] to a specific value of [T].
 *
 * @param T the type of value being chosen.
 * @since %CURRENT_VERSION%
 */
abstract class Chooser<T> {

    /**
     * The currently selected value, or `null` if no choice has been made.
     *
     * @since %CURRENT_VERSION%
     */
    internal var choice: T? = null
        private set

    /**
     * Records the given [choice] as the selected value.
     *
     * @param choice the value to select.
     * @since %CURRENT_VERSION%
     */
    protected fun makeChoice(choice: T) {
        this.choice = choice
    }

}
