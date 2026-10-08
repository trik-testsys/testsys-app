package tech.testsys.web.app.error

import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.expectView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.RouteConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.ContestNotAddedToClassError
import tech.testsys.operation.error.ContestNotAddedToCompetitionError
import tech.testsys.operation.error.ContestNotEnteredError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.ResourceNotInCommittedTaskError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.web.app.MockSpringVaadinTests

@SpringBootTest
class OperationErrorViewTests : MockSpringVaadinTests() {
    @BeforeEach
    fun registerFailingRoutes() {
        RouteConfiguration.forSessionScope().setRoute(FAILING_ROUTE, FailingView::class.java)
        RouteConfiguration.forSessionScope().setRoute(FAILING_CONSTRUCTOR_ROUTE, FailingConstructorView::class.java)
    }

    @ParameterizedTest
    @MethodSource("failures")
    fun `should show the screen and status of the failure kind`(kind: String, status: Int, title: String, pageTitle: String) {
        val ui = UI.getCurrent()

        val result = ui.internals.router.navigate(ui, Location("$FAILING_ROUTE/$kind"), NavigationTrigger.PAGE_LOAD)

        assertEquals(status, result)
        expectView<OperationErrorView>()
        assertEquals(title, _get<H1>().text)
        assertEquals(pageTitle, _get<OperationErrorView>().pageTitle)
    }

    @Test
    fun `should handle a failed operation thrown while the view is created`() {
        val ui = UI.getCurrent()

        val result = ui.internals.router.navigate(ui, Location(FAILING_CONSTRUCTOR_ROUTE), NavigationTrigger.PAGE_LOAD)

        assertEquals(404, result)
        assertEquals("Страница не найдена", _get<H1>().text)
    }

    /** Fails with the operation error named by its parameter. */
    class FailingView : Div(), HasUrlParameter<String> {
        override fun setParameter(event: BeforeEvent, kind: String): Unit = throw operationFailure(ERRORS.getValue(kind))
    }

    /** Fails while Spring creates it, so the failure reaches the router wrapped into a bean creation exception. */
    class FailingConstructorView : Div() {
        init {
            throw operationFailure(TaskNotExistsError(TaskId(1)))
        }
    }

    companion object {
        private const val FAILING_ROUTE = "operation-failure"
        private const val FAILING_CONSTRUCTOR_ROUTE = "operation-failure-on-create"

        private val ERRORS = mapOf(
            "task" to TaskNotExistsError(TaskId(1)),
            "submission" to SubmissionAccessDeniedError(SubmissionId(1)),
            "judge" to MissedJudgeRoleError,
            "reason" to BlankJudgmentReasonError,
            "resource" to ResourceNotInCommittedTaskError(TaskId(1), TaskId(2)),
            "class-contest" to ContestNotAddedToClassError(ClassId(1), ContestId(1)),
            "competition-contest" to ContestNotAddedToCompetitionError(CompetitionId(1), ContestId(1)),
            "not-entered" to ContestNotEnteredError(ContestId(1)),
        )

        @JvmStatic
        fun failures(): List<Arguments> = listOf(
            Arguments.of("task", 404, "Страница не найдена", "Страница не найдена — TestSys"),
            Arguments.of("submission", 403, "Нет доступа", "Нет доступа — TestSys"),
            Arguments.of("judge", 403, "Нет доступа", "Нет доступа — TestSys"),
            Arguments.of("resource", 403, "Нет доступа", "Нет доступа — TestSys"),
            Arguments.of("class-contest", 404, "Страница не найдена", "Страница не найдена — TestSys"),
            Arguments.of("competition-contest", 404, "Страница не найдена", "Страница не найдена — TestSys"),
            Arguments.of("not-entered", 403, "Нет доступа", "Нет доступа — TestSys"),
            Arguments.of(
                "reason",
                400,
                "Не удалось открыть страницу",
                "Не удалось открыть страницу — TestSys",
            ),
        )
    }
}
