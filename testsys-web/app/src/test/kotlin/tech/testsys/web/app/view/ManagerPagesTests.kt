package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ManagerPagesTests {
    @Test
    fun `should quote separators quotes and line breaks with a BOM and CRLF rows`() {
        val actual = csvOf(listOf(listOf("ID", "Псевдоним"), listOf("1", "A;\"B\"\r\nC")))

        assertEquals("\uFEFFID;Псевдоним\r\n1;\"A;\"\"B\"\"\r\nC\"\r\n", actual)
    }

    @Test
    fun `should export the participant header when no participants exist`() {
        assertEquals("\uFEFFID;Псевдоним;Код-доступа\r\n", participantsCsv(emptyList()))
    }
}
