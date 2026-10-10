package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.judgmentOrderData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView

@SpringBootTest
class JudgeSolutionViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var contests: ContestRepository

    @Autowired
    private lateinit var submissions: SubmissionRepository

    @Autowired
    private lateinit var judgmentOrders: JudgmentOrderRepository

    @Test
    fun `should show the details of the submission in read-only fields`() {
        signIn(fixtures.judge())
        val author = student("Ученик Анна")
        val submission = fixtures.gradingSubmission(author, score = 42)

        open(submission)

        assertEquals(submission.id.value.toString(), textField("ID").value)
        assertEquals("Ученик Анна", textField("Автор").value)
        assertEquals(author.id.value.toString(), textField("ID автора").value)
        assertEquals(checkNotNull(tasks.findById(submission.data.task.id)).data.name, textField("Задача").value)
        assertEquals(contestNameOf(submission), textField("Тур").value)
        assertEquals("Python", textField("Язык").value)
        assertEquals("42", textField("Итоговый балл").value)
        assertTrue(textField("ID").isReadOnly)
        assertTrue(downloadLabels().any { label -> "solution.py" in label }, downloadLabels().toString())
    }

    @Test
    fun `should show the status in the head with the author and the task`() {
        signIn(fixtures.judge())
        val submission = fixtures.gradingSubmission(student("Ученик Анна"))

        open(submission)

        val taskName = checkNotNull(tasks.findById(submission.data.task.id)).data.name
        assertTrue("Проверена" in headText(), headText())
        assertEquals("Ученик Анна · Задача «$taskName»", UI.getCurrent()._get<Span> { classes = "ts-page-head__meta" }.text)
    }

    @Test
    fun `should show the score of each polygon with the downloads of its logs and recording`() {
        signIn(fixtures.judge())

        open(fixtures.gradingSubmission(student(), score = 42))

        assertTrue("42" in tableText("Полигон"), tableText("Полигон"))
        assertTrue(downloadLabels().any { label -> "Скачать логи полигона" in label }, downloadLabels().toString())
        assertTrue(downloadLabels().any { label -> "Скачать видеозапись полигона" in label }, downloadLabels().toString())
    }

    @Test
    fun `should list the judgment orders in the order of issue with their judges`() {
        val judge = fixtures.judge().also { judge -> signIn(judge) }
        val other = fixtures.judge()
        val submission = fixtures.gradingSubmission(student())
        issue(submission, judge = other, score = 10, reason = "Первое решение")
        issue(submission, judge = judge, score = 20, reason = "Второе решение")

        open(submission)

        val text = tableText("Обоснование")
        val first = text.indexOf("10Первое решение${other.data.name}")
        val second = text.indexOf("20Второе решение${judge.data.name}")
        assertTrue(first in 0..<second, text)
        assertEquals("20", textField("Итоговый балл").value)
    }

    @Test
    fun `should issue a judgment order from the dialog and show it with the new final score`() {
        val judge = fixtures.judge().also { judge -> signIn(judge) }
        val submission = fixtures.gradingSubmission(student(), score = 42)
        open(submission)

        UI.getCurrent()._get<Button> { text = "Выставить вердикт" }._click()
        openDialog()._get<IntegerField>()._value = 90
        openDialog()._get<TextArea>()._value = "Пересмотрено вручную"
        openDialog()._get<Button> { text = "Выставить" }._click()

        assertTrue("90Пересмотрено вручную${judge.data.name}" in tableText("Обоснование"), tableText("Обоснование"))
        assertEquals("90", textField("Итоговый балл").value)
        assertEquals(1, judgmentOrdersOf(submission))
        assertEquals("Судейский вердикт сохранён", lastToastTitle())
    }

    @Test
    fun `should issue nothing if the score is empty or the reason is blank`() {
        signIn(fixtures.judge())
        val submission = fixtures.gradingSubmission(student())
        open(submission)

        UI.getCurrent()._get<Button> { text = "Выставить вердикт" }._click()
        openDialog()._get<TextArea>()._value = "   "
        openDialog()._get<Button> { text = "Выставить" }._click()

        assertEquals(0, judgmentOrdersOf(submission))
        assertTrue(openDialog()._get<IntegerField>().isInvalid)
        assertTrue(openDialog()._get<TextArea>().isInvalid)
    }

    @Test
    fun `should open the dialog with an empty form again after it was cancelled`() {
        signIn(fixtures.judge())
        open(fixtures.gradingSubmission(student()))
        UI.getCurrent()._get<Button> { text = "Выставить вердикт" }._click()
        openDialog()._get<IntegerField>()._value = 90
        openDialog()._get<TextArea>()._value = "Черновик"
        openDialog()._get<Button> { text = "Отменить" }._click()

        UI.getCurrent()._get<Button> { text = "Выставить вердикт" }._click()

        assertNull(openDialog()._get<IntegerField>().value)
        assertEquals("", openDialog()._get<TextArea>().value)
    }

    @Test
    fun `should show an empty verdict and no issuing for a submission without a successful verdict`() {
        signIn(fixtures.judge())

        open(fixtures.gradingSubmission(student(), isGraded = false))

        val emptyTitles = UI.getCurrent()._find<Span> { classes = "ts-empty__title" }.map { title -> title.text }
        assertTrue("Вердикта нет" in emptyTitles, emptyTitles.toString())
        assertEquals(emptyList<Button>(), UI.getCurrent()._find<Button> { text = "Выставить вердикт" })
        assertEquals("—", textField("Итоговый балл").value)
        assertTrue("В очереди" in headText(), headText())
    }

    @Test
    fun `should show the not found screen for a missing submission`() {
        signIn(fixtures.judge())

        UI.getCurrent().navigate(JudgeSolutionView::class.java, RouteParameters(SUBMISSION_ID_PARAMETER, Long.MAX_VALUE.toString()))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a developer solution test`() {
        signIn(fixtures.judge())

        open(fixtures.developerSolutionTestSubmission(student()))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen to a user without the judge role`() {
        signIn(fixtures.multipleRoleUser())

        open(fixtures.gradingSubmission(student()))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    private fun student(name: String = fixtures.unique("Student")): MultipleRoleUser =
        fixtures.multipleRoleUser(name = name) { roles { student { data = studentData {} } } }

    private fun open(submission: Submission) {
        UI.getCurrent().navigate(JudgeSolutionView::class.java, RouteParameters(SUBMISSION_ID_PARAMETER, submission.id.value.toString()))
    }

    private fun issue(submission: Submission, judge: MultipleRoleUser, score: Int, reason: String) {
        judgmentOrders.save(
            judgmentOrderData {
                this.judge = judge.id
                this.submission = submission.id
                this.score = score
                this.reason = reason
            },
        )
    }

    private fun judgmentOrdersOf(submission: Submission): Int =
        checkNotNull(submissions.findById(submission.id)).data.judgmentOrders.ids.size

    private fun contestNameOf(submission: Submission): String {
        val contestId = checkNotNull(submission.data.kind as? SubmissionKind.Grading).contest.id
        return checkNotNull(contests.findById(contestId)).data.name
    }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun downloadLabels(): List<String> = UI.getCurrent()._find<NativeButton>().mapNotNull { button ->
        button.element.getAttribute("aria-label")
    }

    private fun headText(): String = UI.getCurrent()._get<Div> { classes = "ts-page-head__title-row" }.element.textRecursively

    /** Returns the text of the first table with the column [title]. */
    private fun tableText(title: String): String =
        UI.getCurrent()._find<Table>().first { table -> title in table.element.textRecursively }.element.textRecursively

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }
}
