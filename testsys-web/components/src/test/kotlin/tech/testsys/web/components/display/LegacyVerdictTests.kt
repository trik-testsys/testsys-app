package tech.testsys.web.components.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.find

class LegacyVerdictTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource(
        "Accepted,OK,ok",
        "WrongAnswer,WA,wa",
        "TimeLimitExceeded,TLE,tle",
        "MemoryLimitExceeded,MLE,mle",
        "RuntimeError,RE,re",
        "CompilationError,CE,ce",
        "Queued,…,queue",
    )
    fun `should render every compatibility caption`(value: LegacyVerdict, caption: String, modifier: String) {
        val root = buildTestContent { legacyVerdict(value) }.find("ts-verdict")

        assertEquals(caption, root.element.text)
        assertTrue("ts-verdict--$modifier" in root.element.classList)
    }

    @Test
    fun `should replace the compatibility caption and appearance through its handle`() {
        lateinit var handle: DataHandle<LegacyVerdict>
        val root = buildTestContent { handle = legacyVerdict(LegacyVerdict.Accepted) }.find("ts-verdict")

        handle.data = LegacyVerdict.Queued

        assertEquals("…", root.element.text)
        assertEquals(setOf("ts-verdict", "ts-verdict--queue"), root.element.classList.toSet())
    }
}
