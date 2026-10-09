package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

@SpringBootTest
class JudgeViewTests : MockSpringVaadinTests() {
    @Test
    fun `should show the forbidden screen to a user without the judge role`() {
        signIn(fixtures.multipleRoleUser())

        UI.getCurrent().navigate(JudgeView::class.java, judgeSubmissionsParameters())

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should open the submissions section when no section is given`() {
        signIn(fixtures.judge())

        UI.getCurrent().navigate(JudgeView::class.java)

        assertEquals(JudgeView::class.java, currentView)
        assertEquals("judge/submissions", UI.getCurrent().internals.activeViewLocation.path)
    }

    @Test
    fun `should start the breadcrumbs with the main page`() {
        signIn(fixtures.judge())

        openSubmissions()

        val crumbs = UI.getCurrent()._find<Nav> { classes = "ts-crumbs" }
            .flatMap { nav -> nav._find<RouterLink>() }
            .associate { link -> link.text to link.href }
        assertEquals(mapOf("Главная" to "home"), crumbs)
    }

    @Test
    fun `should show the submission, its author and its total score in a row`() {
        signIn(fixtures.judge())
        val submission = fixtures.gradingSubmission(student("Ученик Иван"), score = 42)

        openSubmissions()

        val text = tableText()
        assertTrue("${submission.id.value}Ученик Иван42" in text, text)
    }

    @Test
    fun `should hide developer solution tests and submissions without a successful verdict`() {
        signIn(fixtures.judge())
        val author = student()
        fixtures.developerSolutionTestSubmission(author)
        fixtures.gradingSubmission(author, isGraded = false)
        openSubmissions()

        field("ID автора")._value = author.id.value.toString()
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        assertTrue("Посылок нет" in tableText(), tableText())
    }

    @Test
    fun `should show only the submission given in the filter after it is applied`() {
        signIn(fixtures.judge())
        val shown = fixtures.gradingSubmission(student())
        val hidden = fixtures.gradingSubmission(student())
        openSubmissions()

        field("ID посылки")._value = shown.id.value.toString()
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue(shown.id.value.toString() in text, text)
        assertTrue(hidden.id.value.toString() !in text, text)
    }

    @Test
    fun `should not apply a submission id that is not a positive integer`() {
        signIn(fixtures.judge())
        val submission = fixtures.gradingSubmission(student())
        openSubmissions()

        field("ID посылки")._value = "-5"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        assertTrue(field("ID посылки").isInvalid)
        assertTrue(submission.id.value.toString() in tableText(), tableText())
    }

    @Test
    fun `should open the submission from its row`() {
        signIn(fixtures.judge())
        val submission = fixtures.gradingSubmission(student())
        openSubmissions()
        val row = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>().first()

        row._fireDomEvent("click", acceptedClick(row))

        assertEquals(JudgeSolutionView::class.java, currentView)
        assertEquals("judge/submissions/${submission.id.value}", UI.getCurrent().internals.activeViewLocation.path)
    }

    private fun student(name: String = fixtures.unique("Student")): MultipleRoleUser =
        fixtures.multipleRoleUser(name = name) { roles { student { data = studentData {} } } }

    private fun openSubmissions() {
        UI.getCurrent().navigate(JudgeView::class.java, judgeSubmissionsParameters())
    }

    private fun field(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun tableText(): String = UI.getCurrent()._get<Table>().element.textRecursively

    /** Data of a click on [row] as the client sends it when the click passes every filter of the row. */
    private fun acceptedClick(row: TableRow): ObjectNode = ObjectMapper().createObjectNode().apply {
        row.element.node.getFeature(ElementListenerMap::class.java).getExpressions("click")
            .forEach { expression -> put(expression, true) }
    }
}
