package tech.testsys.infra.localization.codegen.source

import com.ibm.icu.util.ULocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.files

class RegionFilesTests {

    private val ru = RegionDefinition("RU", "ru-RU", ULocale.forLanguageTag("ru-RU"), comment = null, line = 1)
    private val en = RegionDefinition("EN", "en-US", ULocale.forLanguageTag("en-US"), comment = null, line = 2)

    @Nested
    inner class LoadRegionsTests {

        @Test
        fun `should report a missing regions file`() {
            val errors = mutableListOf<String>()

            val regions = RegionFiles.loadRegions(files("ru-RU/task.properties" to "task.a=x"), errors)

            assertEquals(emptyList<RegionDefinition>(), regions)
            assertEquals(listOf("regions.properties:1: the file is missing; it declares the supported regions"), errors)
        }
    }

    @Nested
    inner class FileSetProblemsTests {

        @Test
        fun `should accept bundle files in the directory of every declared region`() {
            val files = files(
                "regions.properties" to "RU=ru-RU\nEN=en-US",
                "ru-RU/task.properties" to "task.a=x",
                "en-US/task.properties" to "task.a=x",
            )

            val problems = RegionFiles.fileSetProblems(listOf(ru, en), files)

            assertEquals(emptyList<String>(), problems)
        }

        @Test
        fun `should report a directory of an undeclared region`() {
            val files = files(
                "regions.properties" to "RU=ru-RU",
                "ru-RU/task.properties" to "task.a=x",
                "de-DE/task.properties" to "task.a=x",
            )

            val problems = RegionFiles.fileSetProblems(listOf(ru), files)

            assertEquals(
                listOf(
                    "de-DE/task.properties:1: the directory 'de-DE' is not the language tag of a region declared in " +
                        "regions.properties",
                ),
                problems,
            )
        }

        @Test
        fun `should report a declared region without bundle files`() {
            val files = files("regions.properties" to "RU=ru-RU\nEN=en-US", "ru-RU/task.properties" to "task.a=x")

            val problems = RegionFiles.fileSetProblems(listOf(ru, en), files)

            assertEquals(listOf("regions.properties:2: region 'EN' has no bundle files in en-US/"), problems)
        }

        @Test
        fun `should report a message file outside a region directory`() {
            val files = files(
                "regions.properties" to "RU=ru-RU",
                "ru-RU/task.properties" to "task.a=x",
                "RU.properties" to "task.a=x",
            )

            val problems = RegionFiles.fileSetProblems(listOf(ru), files)

            assertEquals(
                listOf("RU.properties:1: a message file must be '<language tag of a region>/<bundle>.properties'"),
                problems,
            )
        }

        @Test
        fun `should report a bundle file whose name is not a bundle`() {
            val files = files("regions.properties" to "RU=ru-RU", "ru-RU/Task.properties" to "task.a=x")

            val problems = RegionFiles.fileSetProblems(listOf(ru), files)

            assertEquals(
                listOf(
                    "ru-RU/Task.properties:1: the file name must be '<bundle>.properties' with a bundle matching " +
                        "[a-z][a-z0-9_]*",
                ),
                problems,
            )
        }

        @Test
        fun `should report a bundle file that another region has and a region lacks`() {
            val files = files(
                "regions.properties" to "RU=ru-RU\nEN=en-US",
                "ru-RU/task.properties" to "task.a=x",
                "ru-RU/tour.properties" to "tour.a=x",
                "en-US/task.properties" to "task.a=x",
            )

            val problems = RegionFiles.fileSetProblems(listOf(ru, en), files)

            assertEquals(listOf("en-US/tour.properties:1: the file is missing; the bundle 'tour' is defined in ru-RU"), problems)
        }
    }

    @Nested
    inner class LoadEntriesTests {

        @Test
        fun `should reject an exclamation mark comment and read the following entry`() {
            val errors = mutableListOf<String>()

            val entries = RegionFiles.loadEntries(ru, files("ru-RU/task.properties" to "! other comment\ntask.a=x"), errors)

            assertEquals(listOf(PropertiesEntry(key = "task.a", value = "x", line = 2, lastLine = 2)), entries)
            assertEquals(listOf("ru-RU/task.properties:1: Comments must start with #"), errors)
        }

        @Test
        fun `should return null when the region has no bundle files`() {
            val errors = mutableListOf<String>()
            val files = files("regions.properties" to "RU=ru-RU\nEN=en-US", "en-US/task.properties" to "task.a=Task")

            val entries = RegionFiles.loadEntries(ru, files, errors)

            assertNull(entries)
            assertEquals(emptyList<String>(), errors)
        }

        @Test
        fun `should return null when one of several bundle files has malformed UTF-8`() {
            val errors = mutableListOf<String>()
            val files = files("ru-RU/task.properties" to "task.a=Задача") +
                ("ru-RU/tour.properties" to byteArrayOf(0xC3.toByte(), 0x28))

            val entries = RegionFiles.loadEntries(ru, files, errors)

            assertNull(entries)
            assertEquals(listOf("ru-RU/tour.properties:1: the file is not valid UTF-8 (MalformedInputException)"), errors)
        }

        @Test
        fun `should collect decoding errors from every malformed bundle file`() {
            val errors = mutableListOf<String>()
            val files = mapOf(
                "ru-RU/tour.properties" to byteArrayOf(0xC3.toByte(), 0x28),
                "ru-RU/task.properties" to byteArrayOf(0xC3.toByte(), 0x28),
            )

            val entries = RegionFiles.loadEntries(ru, files, errors)

            assertNull(entries)
            assertEquals(
                listOf(
                    "ru-RU/task.properties:1: the file is not valid UTF-8 (MalformedInputException)",
                    "ru-RU/tour.properties:1: the file is not valid UTF-8 (MalformedInputException)",
                ),
                errors,
            )
        }

        @Test
        fun `should read the entries of all bundle files of a region in path order`() {
            val errors = mutableListOf<String>()
            val files = files(
                "ru-RU/tour.properties" to "tour.a=Тур",
                "ru-RU/task.properties" to "task.a=Задача\ntask.b=Задачи",
                "en-US/task.properties" to "task.a=Task",
            )

            val entries = RegionFiles.loadEntries(ru, files, errors)

            assertEquals(
                listOf(
                    PropertiesEntry("task.a", "Задача", 1, 1),
                    PropertiesEntry("task.b", "Задачи", 2, 2),
                    PropertiesEntry("tour.a", "Тур", 1, 1),
                ),
                entries,
            )
            assertEquals(emptyList<String>(), errors)
        }

        @Test
        fun `should report a key of another bundle`() {
            val errors = mutableListOf<String>()

            RegionFiles.loadEntries(ru, files("ru-RU/task.properties" to "task.a=Задача\nuser.name=Имя"), errors)

            assertEquals(
                listOf(
                    "ru-RU/task.properties:2: key 'user.name' does not belong to the bundle 'task' of this file; move it " +
                        "to the file of its bundle",
                ),
                errors,
            )
        }
    }
}
