package tech.testsys.infra.localization.codegen.emitter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.LocalizationCodegen
import tech.testsys.infra.localization.codegen.RU_TASK_TERM
import tech.testsys.infra.localization.codegen.bundleFiles
import tech.testsys.infra.localization.codegen.files
import tech.testsys.infra.localization.codegen.generate
import tech.testsys.infra.localization.codegen.ruFiles

class KotlinEmitterTests {

    private fun source(name: String, vararg messages: String): String = generate(
        "regions.properties" to "# Russia: Russian language, Russian Federation conventions.\nRU=ru-RU\nEN=en-US",
        *bundleFiles("ru-RU", messages.toList()).toTypedArray(),
        *bundleFiles("en-US", messages.toList()).toTypedArray(),
    ).single { it.name == name }.toString()

    @Nested
    inner class StructureTests {

        @Test
        fun `should keep generated KDoc descriptions and parameters without since placeholders`() {
            val generated = generate(*ruFiles("task.words={\$count :spellout rules=spellout-numbering}"))
                .associate { it.name to it.toString() }

            assertEquals(false, generated.values.any { "@since" in it })
            assertEquals(true, "Renders the localized message for key 'task.words'." in generated.getValue("Task"))
            assertEquals(true, "@param count value of the MF2 variable '\$count'." in generated.getValue("Task"))
            assertEquals(true, "@property task localized messages of the [Task] bundle." in generated.getValue("Localization"))
        }

        @Test
        fun `should generate SupportedRegion from the regions file`() {
            val source = source("SupportedRegion", "task.a=x")

            assertEquals(
                listOf(true, true, true, true, false),
                listOf(
                    "package tech.testsys.infra.localization.bundle" in source,
                    "@Suppress(\"VERBOSE_DOC\")\npublic enum class SupportedRegion {" in source,
                    "  /**\n   * Russia: Russian language, Russian Federation conventions.\n   */\n  RU," in source,
                    "RU -> ULocale.forLanguageTag(\"ru-RU\")\n    EN -> ULocale.forLanguageTag(\"en-US\")" in source,
                    "@since %CURRENT_VERSION%" in source,
                ),
            )
        }

        @Test
        fun `should generate forRegion with a required time zone and no glossary bundle`() {
            val source = generate(
                *ruFiles(RU_TASK_TERM, "task.a={:term name=task case=nom number=sg}"),
            ).single { it.name == "Localization" }.toString()

            assertEquals(
                listOf(true, false),
                listOf(
                    "public fun forRegion(region: SupportedRegion, timeZone: ZoneId): Localization" in source,
                    "glossary" in source.lowercase(),
                ),
            )
        }

        @Test
        fun `should mark generated internal API with the opt-in marker`() {
            val generated = LocalizationCodegen.generate(files(*ruFiles("task.a=x")), packageName = "x.y")
                .associate { it.name to it.toString() }

            assertEquals(
                listOf(true, true, true, true),
                listOf(
                    "@InternalLocalizationApi\ninternal object LocalizationMessages" in generated.getValue("LocalizationMessages"),
                    "@OptIn(InternalLocalizationApi::class)\npublic class Task @InternalLocalizationApi internal constructor(" in
                        generated.getValue("Task"),
                    "@OptIn(InternalLocalizationApi::class)\n    public fun forRegion(" in generated.getValue("Localization"),
                    "import tech.testsys.infra.localization.InternalLocalizationApi" in generated.getValue("Task"),
                ),
            )
        }

        @Test
        fun `should emit the API into the given package`() {
            val generated = LocalizationCodegen.generate(files(*ruFiles("task.a=x")), packageName = "x.y")

            assertEquals(
                listOf("x.y.Task", "x.y.Localization", "x.y.LocalizationMessages", "x.y.bundle.SupportedRegion"),
                generated.map { "${it.packageName}.${it.name}" },
            )
        }

        @Test
        fun `should not generate a Glossary class`() {
            val names = generate(
                *ruFiles(RU_TASK_TERM, "task.a={:term name=task case=nom number=sg}"),
            ).map { it.name }

            assertEquals(listOf("Task", "Localization", "LocalizationMessages", "SupportedRegion"), names)
        }

        @Test
        fun `should qualify members so that parameters named like them compile`() {
            val source = source(
                "Names",
                "names.reserved={\$region} {\$pattern} {\$timeZone} {\$args} {\$formatter} {\$runtime}",
            )

            assertEquals(
                true,
                "this.runtime.format(\"names.reserved\", this.timeZone, \"region\" to region, \"pattern\" to pattern, " +
                    "\"timeZone\" to timeZone, \"args\" to args, \"formatter\" to formatter, \"runtime\" to runtime)" in source,
            )
        }

        @Test
        fun `should pass a string selector as the lowercase enum name`() {
            val source = source("User", "user.solved=.input {\$gender :string} .match \$gender female {{Она}} * {{Он}}")

            assertEquals(
                listOf(true, true),
                listOf(
                    "public fun solved(gender: SolvedGender): String" in source,
                    "\"gender\" to gender.name.lowercase()" in source,
                ),
            )
        }

        @Test
        fun `should put non-context date zones into the message tables`() {
            val source = source(
                "LocalizationMessages",
                "tour.start={\$start :datetime timeZone=|Europe/Moscow|} {\$end :datetime timeZone=\$zone}",
            )

            assertEquals(
                true,
                "\"tour.start\" to mapOf(\"start\" to DateZone.Fixed(ZoneId.of(\"Europe/Moscow\")), " +
                    "\"end\" to DateZone.Argument(\"zone\"))" in source,
            )
        }
    }
}
