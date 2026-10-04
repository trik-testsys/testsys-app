package tech.testsys.infra.localization.codegen.signature

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.ruEnErrors
import tech.testsys.infra.localization.codegen.ruEnSource

// Cross-region fixtures: the second region EN checks what one region alone cannot show.
class SignatureMergerTests {

    @Test
    fun `should widen Number to Int if another region formats the argument as an integer`() {
        val source = ruEnSource(
            listOf("score.total=Баллы: {\$points :integer}"),
            listOf("score.total=Points: {\$points :number}"),
            "Score",
        )

        assertEquals(true, "public fun total(points: Int): String" in source)
    }

    @Test
    fun `should generate the union of the string selector keys of all regions`() {
        val source = ruEnSource(
            listOf("solution.verdict=.input {\$status :string} .match \$status accepted {{Принято}} * {{Проверяется}}"),
            listOf("solution.verdict=.input {\$status :string} .match \$status rejected {{Rejected}} * {{Checking}}"),
            "Solution",
        )

        assertEquals(true, "public enum class VerdictStatus {\n    ACCEPTED,\n    OTHER,\n    REJECTED,\n  }" in source)
    }

    @Test
    fun `should keep an argument that only one region uses`() {
        val source = ruEnSource(listOf("greeting.hello=Привет, {\$name}!"), listOf("greeting.hello=Hello!"), "Greeting")

        assertEquals(true, "public fun hello(name: String): String" in source)
    }

    @Test
    fun `should reject conflicting argument types across regions`() {
        val errors = ruEnErrors(listOf("tour.start=Начало: {\$start :datetime}"), listOf("tour.start=Start: {\$start}"))

        assertEquals(listOf("EN / tour.start: conflicting types for '\$start': Instant vs String (in RU)"), errors)
    }

    @Test
    fun `should reject a key missing in a region`() {
        val errors = ruEnErrors(listOf("task.a=А", "task.b=Б"), listOf("task.a=A"))

        assertEquals(listOf("EN / task.b: the key is missing in this region (it is defined in RU)"), errors)
    }

    @Test
    fun `should report a missing bundle file once instead of each of its keys`() {
        val errors = ruEnErrors(listOf("task.a=А", "tour.a=Тур", "tour.b=Туры"), listOf("task.a=A"))

        assertEquals(listOf("en-US/tour.properties:1: the file is missing; the bundle 'tour' is defined in ru-RU"), errors)
    }
}
