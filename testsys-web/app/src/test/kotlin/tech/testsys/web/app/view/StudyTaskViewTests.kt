package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import tech.testsys.web.app.error.OperationErrorView
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@SpringBootTest
class StudyTaskViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var study: StudyFixtures

    @Autowired
    private lateinit var submissions: SubmissionRepository

    @Test
    fun `should send only the latest file after repeated upload removal`() {
        val (student, studyClass, contest, task) = entered()
        signIn(student)
        open(studyClass, contest, task)
        StudyPages.removeUpload(StudyPages.upload("first.py", "first".toByteArray()))
        StudyPages.removeUpload(StudyPages.upload("second.py", "second".toByteArray()))
        StudyPages.upload("latest.py", "latest".toByteArray())

        UI.getCurrent()._get<Button> { text = "Отправить" }._click()

        assertEquals(1, submissions.findGradingByContext(authorId = student.id, taskId = task.id, contestId = contest.id).size)
        assertTrue("latest.py" in StudyPages.rowTexts("Результат").single())
    }

    @Test
    fun `should not send a solution removed from the upload list`() {
        val (student, studyClass, contest, task) = entered()
        signIn(student)
        open(studyClass, contest, task)
        val identity = StudyPages.upload("solution.py", "print(1)".toByteArray())
        UI.getCurrent()._get<Upload>()._fireDomEvent(
            "testsys-transfer-remove",
            ObjectMapper().createObjectNode().put("event.detail.identity", identity),
        )

        UI.getCurrent()._get<Button> { text = "Отправить" }._click()

        assertEquals("Выберите файл", StudyPages.lastToastTitle())
        assertEquals(0, submissions.findGradingByContext(authorId = student.id, taskId = task.id, contestId = contest.id).size)
    }

    @Test
    fun `should show the forbidden screen to a student who has not started the contest`() {
        val student = study.student()
        val task = study.task()
        val contest = study.runningContest(tasks = listOf(task))
        val studyClass = study.studentClass(listOf(student), listOf(contest))
        signIn(student)

        open(studyClass, contest, task)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should lead the breadcrumbs of a participant to the cabinet and the contest`() {
        val task = study.task(name = "Лабиринт")
        val contest = study.runningContest(name = "Весенний тур", tasks = listOf(task))
        val participant = study.participant(listOf(contest))
        study.participantEntry(participant, contest)
        signIn(participant)

        UI.getCurrent().navigate(
            ParticipantTaskView::class.java,
            RouteParameters(
                mapOf(STUDY_CONTEST_ID_PARAMETER to contest.id.value.toString(), STUDY_TASK_ID_PARAMETER to task.id.value.toString()),
            ),
        )

        assertEquals("Задача «Лабиринт»", UI.getCurrent()._get<H1>().text)
        assertEquals(
            mapOf("Кабинет Участника" to "participant", "Тур «Весенний тур»" to "participant/contests/${contest.id.value}"),
            StudyPages.crumbs(),
        )
    }

    @Test
    fun `should show the file name and the result of each solution and mark the best one`() {
        val (student, studyClass, contest, task) = entered()
        study.submission(student.id, task, contest, filename = "first.py", score = 30)
        study.submission(student.id, task, contest, filename = "second.py", score = 70)
        study.submission(student.id, task, contest, filename = "third.py")
        signIn(student)

        open(studyClass, contest, task)

        val rows = StudyPages.rowTexts("Результат")
        assertEquals(listOf("first.py30", "second.py70Лучшее", "third.pyВ очереди"), rows.map { row -> row.substringAfter(":").drop(2) })
        assertTrue(Regex("Лучший результат\\W*70").containsMatchIn(StudyPages.pageText()), StudyPages.pageText())
    }

    @Test
    fun `should offer only the languages the task allows`() {
        val (student, studyClass, contest, task) = entered(listOf(TrikSupportedLanguage.VisualLanguage, TrikSupportedLanguage.Python))
        signIn(student)

        open(studyClass, contest, task)

        val language = languageSelect()
        val labels = language.listDataView.items.map(language.itemLabelGenerator::apply).toList()
        assertEquals(listOf("Визуальный язык TRIK Studio", "Python"), labels)
    }

    @Test
    fun `should send the uploaded solution in the only allowed language`() {
        val (student, studyClass, contest, task) = entered()
        signIn(student)
        open(studyClass, contest, task)

        StudyPages.upload("solution.py", "print(1)".toByteArray())
        UI.getCurrent()._get<Button> { text = "Отправить" }._click()

        val sent = submissions.findGradingByContext(authorId = student.id, taskId = task.id, contestId = contest.id)
        assertEquals(1, sent.size)
        assertEquals("Решение отправлено", StudyPages.lastToastTitle())
        assertTrue("solution.py" in StudyPages.rowTexts("Результат").single())
    }

    @Test
    fun `should disable sending after the end of the contest`() {
        val (student, studyClass, contest, task) = entered(contestDuration = Duration.ofHours(1))
        signIn(student)

        open(studyClass, contest, task)

        assertFalse(UI.getCurrent()._get<Button> { text = "Отправить" }.isEnabled)
        assertTrue("Время вышло" in StudyPages.pageText())
    }

    /**
     * Returns a student who started a contest of a task in [languages] in a class; the contest started long ago and lasts
     * [contestDuration], until far in the future without it.
     */
    private fun entered(
        languages: List<TrikSupportedLanguage> = listOf(TrikSupportedLanguage.Python),
        contestDuration: Duration = Duration.between(StudyFixtures.PAST, StudyFixtures.FAR_FUTURE),
    ): StudyContext {
        val student = study.student()
        val task = study.task(languages = languages)
        val contest = study.contest(tasks = listOf(task), startsAt = StudyFixtures.PAST, contestDuration = contestDuration)
        val studyClass = study.studentClass(listOf(student), listOf(contest))
        study.studentEntry(student, studyClass, contest)
        return StudyContext(student, studyClass, contest, task)
    }

    private fun open(studyClass: Class, contest: Contest, task: Task) {
        val parameters = RouteParameters(
            mapOf(
                STUDY_CLASS_ID_PARAMETER to studyClass.id.value.toString(),
                STUDY_CONTEST_ID_PARAMETER to contest.id.value.toString(),
                STUDY_TASK_ID_PARAMETER to task.id.value.toString(),
            ),
        )
        UI.getCurrent().navigate(StudentTaskView::class.java, parameters)
    }

    @Suppress("UNCHECKED_CAST")
    private fun languageSelect(): Select<TrikSupportedLanguage> =
        checkNotNull(UI.getCurrent()._get<Select<*>>() as? Select<TrikSupportedLanguage>)

    private data class StudyContext(val student: MultipleRoleUser, val studyClass: Class, val contest: Contest, val task: Task)
}
