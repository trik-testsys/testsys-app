package tech.testsys.web.app.error

import com.github.mvysny.kaributesting.v10._expectNone
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.VaadinSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
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
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.StatementNotUploadedToTaskError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.PostgresTestConfiguration

@Import(PostgresTestConfiguration::class)
@SpringBootTest
class OperationErrorHandlerTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var handler: OperationErrorHandler

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

        assertEquals("Объект не найден", _get<Span> { classes = "ts-toast__title" }.text)
    }

    @Test
    fun `should leave other exceptions to the default handler without a toast`() {
        handler.error(ErrorEvent(IllegalStateException("not an operation")))

        _expectNone<Div> { classes = "ts-toast" }
    }

    @Test
    fun `should become the error handler of a new session`() {
        val sessionHandler = VaadinSession.getCurrent().errorHandler

        assertSame(handler, sessionHandler)
    }

    companion object {
        @JvmStatic
        fun failures(): List<Arguments> = listOf(
            Arguments.of(TaskNotExistsError(TaskId(1)), "Объект не найден"),
            Arguments.of(SubmissionAccessDeniedError(SubmissionId(1)), "Нет доступа"),
            Arguments.of(MissedJudgeRoleError, "Нет нужной роли"),
            Arguments.of(
                StatementNotUploadedToTaskError(TaskId(1), StatementId(1)),
                "Ресурс недоступен в этой задаче",
            ),
            Arguments.of(BlankJudgmentReasonError, "Действие не выполнено"),
        )
    }
}
