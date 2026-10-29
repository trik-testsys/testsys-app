package tech.testsys.infra.localization.codegen.source

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PropertiesScannerTests {

    @Nested
    inner class DecodeTests {

        @ParameterizedTest
        @CsvSource("empty, 1", "lf, 2", "cr, 2", "crlf, 2", "mixed, 5", "consecutive, 4")
        fun `should count all line break forms before malformed UTF-8`(kind: String, line: Int) {
            val prefixes = mapOf(
                "empty" to "",
                "lf" to "а\n",
                "cr" to "а\r",
                "crlf" to "а\r\n",
                "mixed" to "а\rб\nв\r\nг\n",
                "consecutive" to "\r\n\r\n\r",
            )
            val errors = mutableListOf<String>()
            val bytes = prefixes.getValue(kind).toByteArray() + byteArrayOf(0xC3.toByte(), 0x28)

            val text = PropertiesScanner.decode("ru-RU/task.properties", bytes, errors)

            assertNull(text)
            assertEquals(listOf("ru-RU/task.properties:$line: the file is not valid UTF-8 (MalformedInputException)"), errors)
        }

        @Test
        fun `should decode strict UTF-8`() {
            val errors = mutableListOf<String>()

            val text = PropertiesScanner.decode("ru-RU/task.properties", "task.a=Задача".toByteArray(), errors)

            assertEquals("task.a=Задача" to emptyList<String>(), text to errors)
        }

        @Test
        fun `should report a BOM`() {
            val errors = mutableListOf<String>()
            val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "task.a=x".toByteArray()

            val text = PropertiesScanner.decode("ru-RU/task.properties", bytes, errors)

            assertNull(text)
            assertEquals(listOf("ru-RU/task.properties:1: the file starts with a UTF-8 BOM; save it as UTF-8 without BOM"), errors)
        }

        @Test
        fun `should report malformed UTF-8 with its line`() {
            val errors = mutableListOf<String>()
            val bytes = "task.a=x\ntask.b=".toByteArray() + byteArrayOf(0xC3.toByte(), 0x28)

            val text = PropertiesScanner.decode("ru-RU/task.properties", bytes, errors)

            assertNull(text)
            assertEquals(listOf("ru-RU/task.properties:2: the file is not valid UTF-8 (MalformedInputException)"), errors)
        }
    }

    @Nested
    inner class ScanTests {

        @Test
        fun `should join continued lines and skip comments`() {
            val errors = mutableListOf<String>()
            val text = "# comment\n# other comment\n\ntask.a=.input {\$n :integer} \\\n    {{x}}\ntask.b=y"

            val entries = PropertiesScanner.scan("ru-RU/task.properties", text, errors)

            assertEquals(
                listOf(PropertiesEntry("task.a", ".input {\$n :integer} {{x}}", 4, 5), PropertiesEntry("task.b", "y", 6, 6)),
                entries,
            )
            assertEquals(emptyList<String>(), errors)
        }

        @ParameterizedTest
        @ValueSource(strings = ["! other comment", "!", "!task.a=x", "  ! comment", "\t! comment", "\u000c! comment"])
        fun `should reject a line starting with an exclamation mark`(line: String) {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", line, errors)

            assertEquals(emptyList<PropertiesEntry>(), entries)
            assertEquals(listOf("ru-RU/task.properties:1: Comments must start with #"), errors)
        }

        @Test
        fun `should reject an exclamation mark line ending with a backslash without continuing it`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "! comment \\\ntask.a=x", errors)

            assertEquals(listOf(PropertiesEntry("task.a", "x", 2, 2)), entries)
            assertEquals(listOf("ru-RU/task.properties:1: Comments must start with #"), errors)
        }

        @Test
        fun `should keep an exclamation mark at the start of a continued value as text`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=first \\\n    ! second", errors)

            assertEquals(listOf(PropertiesEntry("task.a", "first ! second", 1, 2)), entries)
            assertEquals(emptyList<String>(), errors)
        }

        @Test
        fun `should keep an exclamation mark in an ordinary value as text`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=! text!", errors)

            assertEquals(listOf(PropertiesEntry("task.a", "! text!", 1, 1)), entries)
            assertEquals(emptyList<String>(), errors)
        }

        @Test
        fun `should report a duplicate key and keep its first definition`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=first\ntask.b=b\ntask.a=second", errors)

            assertEquals(listOf("first", "b"), entries.map { it.value })
            assertEquals(listOf("ru-RU/task.properties:3: duplicate key 'task.a' (first defined at line 1)"), errors)
        }

        @Test
        fun `should report an empty value`() {
            val errors = mutableListOf<String>()

            PropertiesScanner.scan("ru-RU/task.properties", "task.a=", errors)

            assertEquals(listOf("ru-RU/task.properties:1: key 'task.a' has an empty value"), errors)
        }

        @Test
        fun `should report trailing whitespace on the last line of the value`() {
            val errors = mutableListOf<String>()

            PropertiesScanner.scan("ru-RU/task.properties", "task.a=first \\\n  second ", errors)

            assertEquals(listOf("ru-RU/task.properties:2: the value of 'task.a' ends with whitespace"), errors)
        }

        @Test
        fun `should keep an escaped backslash at the end of a line as text`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=a\\\\\ntask.b=b", errors)

            assertEquals(listOf("task.a" to "a\\", "task.b" to "b"), entries.map { it.key to it.value })
        }

        @Test
        fun `should split CRLF and CR lines and join continued lines`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=first \\\r\n    second\rtask.b=b", errors)

            assertEquals(listOf(PropertiesEntry("task.a", "first second", 1, 2), PropertiesEntry("task.b", "b", 3, 3)), entries)
            assertEquals(emptyList<String>(), errors)
        }

        @Test
        fun `should not continue a comment line ending with a backslash`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "# comment \\\ntask.a=x", errors)

            assertEquals(listOf(PropertiesEntry("task.a", "x", 2, 2)), entries)
        }

        @Test
        fun `should unescape a unicode escape in a value`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=\\u0417адача", errors)

            assertEquals(listOf("task.a" to "Задача"), entries.map { it.key to it.value })
        }

        @Test
        fun `should report a malformed unicode escape with its line`() {
            val errors = mutableListOf<String>()

            val entries = PropertiesScanner.scan("ru-RU/task.properties", "task.a=x\ntask.b=C:\\users", errors)

            assertEquals(listOf("task.a"), entries.map { it.key })
            assertEquals(
                listOf(
                    "ru-RU/task.properties:2: malformed \\uXXXX escape (Malformed \\uxxxx encoding.); write the character " +
                        "itself, exactly four hex digits after \\u or \\\\ for a backslash",
                ),
                errors,
            )
        }
    }
}
