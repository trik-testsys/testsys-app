package tech.testsys.web.app.error

import com.github.mvysny.kaributesting.v10._expectNone
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.VaadinSession
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.StatementNotUploadedToTaskError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.TrikStudioVersionNotRegisteredError
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.PostgresTestConfiguration
import tech.testsys.web.app.service.judge.JudgeService

@Import(PostgresTestConfiguration::class)
@SpringBootTest
class OperationErrorHandlerTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var handler: OperationErrorHandler

    @Autowired
    private lateinit var judges: JudgeService

    @Autowired
    private lateinit var registry: MeterRegistry

    @ParameterizedTest
    @MethodSource("failures")
    fun `should show an error toast titled by the kind of the failed operation`(error: OperationError, title: String) {
        handler.error(ErrorEvent(operationFailure(error)))

        assertTrue("ts-toast--error" in _get<Div> { classes = "ts-toast" }.element.classList)
        assertEquals(title, _get<Span> { classes = "ts-toast__title" }.text)
    }

    @Test
    fun `should show a toast if the failed operation is wrapped into another exception`() {
        val wrapped = IllegalStateException("listener failed", operationFailure(TaskNotExistsError(TaskId(1))))

        handler.error(ErrorEvent(wrapped))

        assertEquals("Не найдено: возможно, это уже удалили", _get<Span> { classes = "ts-toast__title" }.text)
    }

    @Test
    fun `should leave other exceptions to the default handler without a toast`() {
        handler.error(ErrorEvent(IllegalStateException("not an operation")))

        _expectNone<Div> { classes = "ts-toast" }
    }

    @Test
    fun `should count an unexpected exception before passing it to the default handler`() {
        val before = technicalFailures(stage = "event")

        handler.error(ErrorEvent(IllegalStateException("not an operation")))

        assertEquals(before + 1, technicalFailures(stage = "event"))
    }

    @Test
    fun `should not count a failed operation as a technical failure`() {
        val before = technicalFailures(stage = "event")

        handler.error(ErrorEvent(operationFailure(TaskNotExistsError(TaskId(1)))))

        assertEquals(before, technicalFailures(stage = "event"))
    }

    @Test
    fun `should count an exception once when the service boundary already counted it`() {
        val beforeService = technicalFailures(stage = "service")
        val beforeEvent = technicalFailures(stage = "event")
        val failure = assertThrows(IllegalStateException::class.java) { judges.viewSolution(SubmissionId(1)) }

        handler.error(ErrorEvent(failure))

        assertEquals(beforeService + 1, technicalFailures(stage = "service"))
        assertEquals(beforeEvent, technicalFailures(stage = "event"))
    }

    @Test
    fun `should become the error handler of a new session`() {
        val sessionHandler = VaadinSession.getCurrent().errorHandler

        assertSame(handler, sessionHandler)
    }

    private fun technicalFailures(stage: String): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
        .tags(TestSysMetrics.SOURCE, "main", TestSysMetrics.STAGE, stage)
        .counters().sumOf { counter -> counter.count() }

    companion object {
        @JvmStatic
        fun failures(): List<Arguments> = listOf(
            Arguments.of(TaskNotExistsError(TaskId(1)), "Не найдено: возможно, это уже удалили"),
            Arguments.of(
                TrikStudioVersionNotRegisteredError(TrikStudioVersion(tag = "trik-studio:old", version = "1.0")),
                "Список версий TRIK Studio устарел: обновите страницу",
            ),
            Arguments.of(SubmissionAccessDeniedError(SubmissionId(1)), "Нет доступа"),
            Arguments.of(MissedJudgeRoleError, "У вас нет нужной роли"),
            Arguments.of(
                StatementNotUploadedToTaskError(TaskId(1), StatementId(1)),
                "Ресурс недоступен в этой задаче",
            ),
            Arguments.of(BlankJudgmentReasonError, "Не удалось выполнить действие"),
        )
    }
}
