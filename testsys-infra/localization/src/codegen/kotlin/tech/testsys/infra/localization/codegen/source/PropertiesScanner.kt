package tech.testsys.infra.localization.codegen.source

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.fileError
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * One `key=value` entry of a `.properties` file.
 *
 * @property line the first physical line of the entry.
 * @property lastLine the last physical line of the entry (differs from [line] for continued values).
 */
internal data class PropertiesEntry(val key: String, val value: String, val line: Int, val lastLine: Int)

/** Reads localization `.properties` files strictly, reporting problems as `<file>:<line>: <problem>`. */
internal object PropertiesScanner {
    // Properties.load silently replaces malformed bytes, keeps the last of duplicate keys and keeps invisible
    // trailing whitespace; this scanner reports them instead.

    private const val BYTE_ORDER_MARK = 0xFEFF

    private val bom = Char(BYTE_ORDER_MARK).toString().toByteArray(StandardCharsets.UTF_8)
    private val lineBreak = Regex("\r\n|\r|\n")

    /** Decodes [bytes] of [fileName] as strict UTF-8 without a BOM; returns `null` and reports into [errors] otherwise. */
    fun decode(fileName: String, bytes: ByteArray, errors: MutableList<String>): String? {
        if (bytes.size >= bom.size && bytes.copyOfRange(0, bom.size).contentEquals(bom)) {
            errors += fileError(fileName, 1, Problems.Files.BOM)
            return null
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val input = ByteBuffer.wrap(bytes)
        return try {
            decoder.decode(input).toString()
        } catch (e: CharacterCodingException) {
            val line = lineNumber(bytes, input.position())
            errors += fileError(fileName, line, Problems.Files.notUtf8(e.javaClass.simpleName))
            null
        }
    }

    /**
     * Splits [text] of [fileName] into entries in file order, keeping the first definition of duplicate keys and reporting
     * duplicate keys, empty values and trailing whitespace into [errors]. Only `#` starts a comment; standalone lines
     * starting with `!` are rejected without continuation, while `!` in a continued value remains text.
     */
    fun scan(fileName: String, text: String, errors: MutableList<String>): List<PropertiesEntry> {
        val lines = text.split(lineBreak)
        val entries = mutableListOf<PropertiesEntry>()
        val firstLines = mutableMapOf<String, Int>()
        var index = 0
        while (index < lines.size) {
            if (lines[index].trimStart(' ', '\t', '\u000c').startsWith('!')) {
                errors += fileError(fileName, index + 1, Problems.Files.COMMENTS_START_WITH_HASH)
                index++
                continue
            }
            val start = index
            index = logicalLineEnd(lines, index) + 1
            val chunk = lines.subList(start, index)
            if (isBlankOrComment(chunk.first())) continue
            val entry = parseEntry(fileName, chunk, start + 1, errors) ?: continue
            val firstLine = firstLines[entry.key]
            when {
                firstLine != null -> errors += fileError(fileName, entry.line, Problems.Files.duplicateKey(entry.key, firstLine))
                else -> {
                    firstLines[entry.key] = entry.line
                    entries += entry
                    errors += valueProblems(fileName, entry)
                }
            }
        }
        return entries
    }

    private fun valueProblems(fileName: String, entry: PropertiesEntry): List<String> = when {
        entry.value.isEmpty() -> listOf(fileError(fileName, entry.line, Problems.Files.emptyValue(entry.key)))
        entry.value.last().isWhitespace() -> listOf(fileError(fileName, entry.lastLine, Problems.Files.trailingWhitespace(entry.key)))
        else -> emptyList()
    }

    private fun lineNumber(bytes: ByteArray, end: Int): Int = 1 + (0 until end).count { index ->
        bytes[index] == '\r'.code.toByte() ||
            (bytes[index] == '\n'.code.toByte() && (index == 0 || bytes[index - 1] != '\r'.code.toByte()))
    }

    // A comment line never continues, even if it ends with a backslash; any other line continues while it ends
    // with an odd number of backslashes, exactly as in Properties.load.
    private fun logicalLineEnd(lines: List<String>, start: Int): Int {
        if (isBlankOrComment(lines[start])) return start
        var end = start
        while (end < lines.lastIndex && endsWithContinuation(lines[end])) end++
        return end
    }

    private fun isBlankOrComment(line: String): Boolean {
        val trimmed = line.trimStart(' ', '\t', '\u000c')
        return trimmed.isEmpty() || trimmed.startsWith('#')
    }

    private fun endsWithContinuation(line: String): Boolean = line.takeLastWhile { it == '\\' }.length % 2 == 1

    // Each logical line is loaded on its own by Properties, so keys and values are unescaped exactly as at runtime.
    // Properties rejects a malformed \uXXXX escape with an IllegalArgumentException.
    private fun parseEntry(fileName: String, chunk: List<String>, firstLine: Int, errors: MutableList<String>): PropertiesEntry? {
        val properties = Properties()
        try {
            properties.load(StringReader(chunk.joinToString("\n")))
        } catch (e: IllegalArgumentException) {
            errors += fileError(fileName, firstLine, Problems.Files.malformedEscape(e.message))
            return null
        }
        val (key, value) = properties.entries.singleOrNull() ?: return null
        return PropertiesEntry(key = key.toString(), value = value.toString(), line = firstLine, lastLine = firstLine + chunk.size - 1)
    }
}
