package tech.testsys.infra.localization.codegen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LocalizationCodegenTests {

    private val deadlineInDays = ".input {\$days :integer} .match \$days " +
        "one {{через {\$days} день}} few {{через {\$days} дня}} many {{через {\$days} дней}} * {{через {\$days} дней}}"

    @Test
    fun `should generate the bundles, Localization, the message tables and SupportedRegion`() {
        val generated = generate(
            "regions.properties" to "RU=ru-RU",
            "ru-RU/task.properties" to "task.deadline.passed=Дедлайн прошёл",
        )

        assertEquals(
            listOf("Task", "Localization", "LocalizationMessages", "SupportedRegion"),
            generated.map { it.name },
        )
    }

    // The acceptance checks of the task, on copies of messages of the example set.
    @Nested
    inner class BuildBreakingTests {

        @Test
        fun `should fail if a reachable category is removed from an example message`() {
            val errors = ruErrors("task.deadline.in_days=" + deadlineInDays.replace("few {{через {\$days} дня}} ", ""))

            assertEquals(
                listOf("RU / task.deadline.in_days: category 'few' of '\$days' has no explicit variant (falls back to '*')"),
                errors,
            )
        }

        @Test
        fun `should fail if minimumIntegerDigits is added to an example message`() {
            val errors = ruErrors("contest.season=Сезон {\$year :integer useGrouping=never minimumIntegerDigits=3}")

            assertEquals(listOf("RU / contest.season: option 'minimumIntegerDigits' of ':integer' is not honoured by ICU4J 78.1"), errors)
        }

        @Test
        fun `should fail if an example key is duplicated`() {
            val errors = ruErrors(
                "task.deadline.passed=Дедлайн прошёл",
                "task.deadline.in_days=$deadlineInDays",
                "task.deadline.passed=Срок прошёл",
            )

            assertEquals(listOf("ru-RU/task.properties:3: duplicate key 'task.deadline.passed' (first defined at line 1)"), errors)
        }

        @Test
        fun `should accumulate the errors of all files and messages`() {
            val errors = ruErrors("task.a={\$x :number foo=bar}", "task.b={\$d :date fields=month-day}", "task.a=again")

            assertEquals(
                listOf(
                    "ru-RU/task.properties:3: duplicate key 'task.a' (first defined at line 1)",
                    "RU / task.a: unknown option 'foo' of ':number'",
                    "RU / task.b: option 'fields' of ':date' is not honoured by ICU4J 78.1 without 'length'",
                ),
                errors,
            )
        }
    }
}
