package tech.testsys.domain.contract

import tech.testsys.domain.model.Describable

interface Violation

fun interface Checker<in T : Describable> {
    fun check(target: T): List<Violation>
}

infix fun <T : Describable> Checker<T>.and(other: Checker<T>): Checker<T> = Checker { target ->
    check(target) + other.check(target)
}

fun <T : Describable> allOf(vararg checkers: Checker<T>): Checker<T> = Checker { target ->
    checkers.flatMap { it.check(target) }
}
