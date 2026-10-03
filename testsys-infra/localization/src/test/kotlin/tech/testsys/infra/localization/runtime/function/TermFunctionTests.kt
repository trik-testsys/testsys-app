package tech.testsys.infra.localization.runtime.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.localization.InternalLocalizationApi
import tech.testsys.infra.localization.runtime.MOSCOW
import tech.testsys.infra.localization.runtime.ruRuntime

@OptIn(InternalLocalizationApi::class)
class TermFunctionTests {

    @Test
    fun `should read the term name from the option if an argument is also called name`() {
        val runtime = ruRuntime("key" to "{\$name}: {\$count :term name=user case=nom}")

        val text = runtime.format("key", MOSCOW, "name" to "task", "count" to 2)

        assertEquals("task: Пользователя", text)
    }

    @ParameterizedTest
    @CsvSource("2, Пользователь", "5, Пользователя", "6, Пользователей")
    fun `should apply the inherited subtract of an offset operand`(count: Int, expected: String) {
        val runtime = ruRuntime(
            "key" to ".input {\$count :integer} .local \$others = {\$count :offset subtract=1} " +
                "{{{\$others :term name=user case=nom}}}",
        )

        assertEquals(expected, runtime.format("key", MOSCOW, "count" to count))
    }

    @Test
    fun `should take the form from the number option if the term has no operand`() {
        val runtime = ruRuntime("key" to "{:term name=task case=gen number=pl}")

        assertEquals("Задач", runtime.format("key", MOSCOW))
    }
}
