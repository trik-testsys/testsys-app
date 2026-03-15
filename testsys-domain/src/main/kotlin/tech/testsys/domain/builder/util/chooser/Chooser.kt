package tech.testsys.domain.builder.util.chooser

abstract class Chooser<T> {

    internal var choice: T? = null
        private set

    protected fun makeChoice(choice: T) {
        this.choice = choice
    }

}
