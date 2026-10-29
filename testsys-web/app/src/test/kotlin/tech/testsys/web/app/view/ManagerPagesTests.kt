package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.ZoneOffset

class ManagerPagesTests {
    @Test
    fun `should quote separators quotes and line breaks with a BOM and CRLF rows`() {
        val actual = csvOf(listOf(listOf("ID", "Псевдоним"), listOf("1", "A;\"B\"\r\nC")))

        assertEquals("﻿ID;Псевдоним\r\n1;\"A;\"\"B\"\"\r\nC\"\r\n", actual)
    }

    @Test
    fun `should export the participant header when no participants exist`() {
        val texts = Localization.forRegion(SupportedRegion.RU, ZoneOffset.UTC).pageManager

        assertEquals("﻿ID;Псевдоним;Код-доступа\r\n", participantsCsv(emptyList(), texts))
    }
}
