package tech.testsys.web.components.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.testTexts

class TableLayoutTests {
    @Test
    fun `should reserve checkbox and menu fractions before the remaining column`() {
        val spec = TableScope<Int>(testTexts, gridColumns = 8, selectionSize = 1).apply {
            codeColumn("ID", size = 2) { row -> row.toString() }
            textColumn("Name") { row -> row.toString() }
            menuColumn { item("Open") {} }
        }.spec()

        assertEquals(listOf(1, 2, 4, 1), spec.layout.sizes)
    }

    @Test
    fun `should preserve unused fractions of an explicitly sized row`() {
        val spec = TableScope<Int>(testTexts, gridColumns = 8).apply { textColumn("Name", size = 3) { "Name" } }.spec()

        assertEquals(8, spec.layout.gridColumns)
        assertEquals(listOf(3), spec.layout.sizes)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 24])
    fun `should accept minimum and maximum explicit fractions`(size: Int) {
        val spec = TableScope<Int>(testTexts).apply { textColumn("Name", size = size) { "Name" } }.spec()

        assertEquals(listOf(size), spec.layout.sizes)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 25])
    fun `should reject a size outside the containing grid`(size: Int) {
        assertThrows<IllegalArgumentException> {
            TableScope<Int>(testTexts).apply { textColumn("Name", size = size) { "Name" } }.spec()
        }
    }

    @Test
    fun `should reject overflow including utility fractions`() {
        assertThrows<IllegalStateException> {
            TableScope<Int>(testTexts, selectionSize = 1).apply {
                textColumn("Name", size = 23) { "Name" }
                menuColumn { item("Open") {} }
            }.spec()
        }
    }

    @Test
    fun `should reject an ordinary column after a remaining column`() {
        assertThrows<IllegalStateException> {
            TableScope<Int>(testTexts).apply {
                textColumn("Name") { "Name" }
                numberColumn("Score", size = 1) { 0 }
            }
        }
    }

    @Test
    fun `should reject overflow at the largest logical capacity`() {
        assertThrows<IllegalStateException> { resolveTableLayout(Int.MAX_VALUE, listOf(Int.MAX_VALUE, Int.MAX_VALUE)) }
    }

    @Test
    fun `should retain all task columns in an expanded matrix`() {
        val spec = TableScope<Int>(testTexts, gridColumns = 206).apply {
            textColumn("Participant", size = 6) { "Name" }
            repeat(100) { index -> numberColumn("Task $index", size = 2) { index } }
        }.spec()

        assertEquals(101, spec.columns.size)
        assertEquals(206, spec.layout.sizes.sum())
        assertEquals("Task 99", spec.columns.last().title)
    }
}
