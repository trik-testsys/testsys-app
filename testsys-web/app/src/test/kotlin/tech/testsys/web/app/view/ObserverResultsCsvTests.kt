package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.operation.user.ObserverResult
import java.time.ZoneOffset

class ObserverResultsCsvTests {
    private val texts = Localization.forRegion(SupportedRegion.RU, ZoneOffset.UTC).pageObserver

    @Test
    fun `should write headings and scores with byte order mark and escaped CRLF rows`() {
        val result = ObserverResult(
            columns = listOf(ObserverResult.Column("Тур;\"А", ContestId(19), "Задача", TaskId(2))),
            rows = listOf(
                ObserverResult.Row(
                    participantId = SingleRoleUserId(17),
                    scores = listOf(42),
                ),
                ObserverResult.Row(
                    participantId = SingleRoleUserId(18),
                    scores = listOf(null),
                ),
            ),
        )

        val csv = observerResultsCsv(result, texts)

        assertEquals(
            "\uFEFFID участника;\"Тур;\"\"А - Задача\"\r\n" +
                "17;42\r\n18;\r\n",
            csv,
        )
    }

    @Test
    fun `should write only a header when no competition is accessible`() {
        val csv = observerResultsCsv(ObserverResult(columns = emptyList(), rows = emptyList()), texts)

        assertEquals("\uFEFFID участника\r\n", csv)
    }
}
