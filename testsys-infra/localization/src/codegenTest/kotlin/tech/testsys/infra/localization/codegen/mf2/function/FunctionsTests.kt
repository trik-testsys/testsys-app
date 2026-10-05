package tech.testsys.infra.localization.codegen.mf2.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class FunctionsTests {

    // Every object of the sealed hierarchy, including those under an intermediate sealed class.
    private fun functionObjects(type: KClass<out Mf2Function>): Set<Mf2Function> =
        type.sealedSubclasses.flatMap { subclass -> subclass.objectInstance?.let(::listOf) ?: functionObjects(subclass) }.toSet()

    @Test
    fun `should register every function object`() {
        assertEquals(functionObjects(Mf2Function::class), Functions.all.toSet())
    }

    @Test
    fun `should give every function a unique name`() {
        assertEquals(Functions.all.size, Functions.byName.size)
    }

    @Test
    fun `should derive the selector functions from their selection`() {
        assertEquals(listOf("string", "integer", "number", "percent", "offset"), Functions.selectors.map { it.name })
    }

    @Test
    fun `should derive the custom functions`() {
        assertEquals(listOf("term", "spellout", "ordinal", "unit"), Functions.custom.map { it.name })
    }

    @Test
    fun `should derive the numeric built-in functions`() {
        assertEquals(setOf("integer", "number", "percent", "currency", "offset"), Functions.numericBuiltIns.map { it.name }.toSet())
    }
}
