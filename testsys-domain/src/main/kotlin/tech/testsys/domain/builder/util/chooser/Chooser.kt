package tech.testsys.domain.builder.util.chooser

abstract class Chooser<T> {

    private var _choice: T? = null
    internal val choice: T?
        get() = _choice

    protected fun makeChoice(choice: T) {
        _choice = choice
    }

}
