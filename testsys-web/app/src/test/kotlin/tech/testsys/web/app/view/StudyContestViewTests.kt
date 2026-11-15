package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.RouteParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.task.Contest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyService
import java.time.Clock
import java.time.Duration

@SpringBootTest
class StudyContestViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var study: StudyFixtures

    @Autowired
    private lateinit var studentEntries: StudentContestEntryRepository

    @Autowired
    private lateinit var participantEntries: ParticipantContestEntryRepository

    @Autowired
    private lateinit var studyService: StudyService

    @Autowired
    private lateinit var studentService: StudentService

    @Autowired
    private lateinit var headers: CabinetHeaders

    @Autowired
    private lateinit var clock: Clock

    @Nested
    inner class StudentTests {
        @Test
        fun `should lead the breadcrumbs to the cabinet and the class`() {
            val student = study.student()
            val contest = study.runningContest(name = "Весенний тур")
            val studyClass = study.studentClass(listOf(student), listOf(contest), name = "Кружок")
            signIn(student)

            open(studyClass, contest)

            assertEquals("Тур «Весенний тур»", UI.getCurrent()._get<H1>().text)
            assertEquals(
                mapOf("Кабинет Ученика" to "student", "Класс «Кружок»" to "student/classes/${studyClass.id.value}"),
                StudyPages.crumbs(),
            )
        }

        @Test
        fun `should title the browser tab of the contest`() {
            val student = study.student()
            val contest = study.runningContest()
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            signIn(student)

            open(studyClass, contest)

            assertEquals("Тур", assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
        }

        @Test
        fun `should keep the title and show a load error in the details and the tasks if the contest fails to load`() {
            val student = study.student()
            val contest = study.runningContest(name = "Весенний тур", tasks = listOf(study.task()))
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            study.studentEntry(student, studyClass, contest)
            signIn(student)
            val service = mockk<StudyService>()
            every { service.checkContestAccess(studyClass.id, contest.id) } returns
                studyService.checkContestAccess(studyClass.id, contest.id)
            every { service.viewContest(studyClass.id, contest.id) } throws IllegalStateException("Contest storage unavailable")
            val view = StudentContestView(headers, clock, service, studentService)
            val event = mockk<BeforeEnterEvent>()
            every { event.routeParameters } returns studentContestParameters(studyClass.id, contest.id)
            UI.getCurrent().removeAll()
            UI.getCurrent().add(view)

            view.beforeEnter(event)

            assertEquals("Тур «Весенний тур»", view._get<H1>().text)
            assertEquals(2, Regex("Не удалось загрузить").findAll(view.element.textRecursively).count())
            assertTrue(view._find<Button> { text = "Начать Тур" }.isEmpty())
        }

        @Test
        fun `should keep the start disabled before the start of the contest`() {
            val student = study.student()
            val contest = study.contest(startsAt = StudyFixtures.FAR_FUTURE, contestDuration = Duration.ofHours(1))
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            signIn(student)

            open(studyClass, contest)

            assertFalse(startAction().isEnabled)
        }

        @Test
        fun `should save the first entry after the confirmation and hide the start`() {
            val student = study.student()
            val contest = study.runningContest()
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            signIn(student)
            open(studyClass, contest)

            startAction()._click()
            UI.getCurrent()._get<Button> { text = "Начать" }._click()

            assertNotNull(studentEntries.findByContext(userId = student.id, studyClassId = studyClass.id, contestId = contest.id))
            assertTrue(UI.getCurrent()._find<Button> { text = "Начать Тур" }.isEmpty())
            assertEquals("Тур начат", lastToastTitle())
        }

        @Test
        fun `should not open a task before the first entry`() {
            val student = study.student()
            val contest = study.runningContest(tasks = listOf(study.task()))
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            signIn(student)
            open(studyClass, contest)

            assertTrue(UI.getCurrent()._find<Table>().isEmpty())
            assertEquals(StudentContestView::class.java, currentView)
        }

        @Test
        fun `should open a task after the first entry`() {
            val student = study.student()
            val contest = study.runningContest(tasks = listOf(study.task()))
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            study.studentEntry(student, studyClass, contest)
            signIn(student)
            open(studyClass, contest)

            StudyPages.clickRow("Название")

            assertEquals(StudentTaskView::class.java, currentView)
        }

        @Test
        fun `should keep tasks hidden before start despite a saved entry`() {
            val student = study.student()
            val contest = study.contest(tasks = listOf(study.task()), startsAt = StudyFixtures.FAR_FUTURE)
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            study.studentEntry(student, studyClass, contest)
            signIn(student)

            open(studyClass, contest)

            assertTrue(UI.getCurrent()._find<Table>().isEmpty())
        }

        @Test
        fun `should show task score and latest status and open tasks after individual deadline`() {
            val student = study.student()
            val task = study.task()
            val contest = study.contest(tasks = listOf(task), startsAt = StudyFixtures.PAST, attemptDuration = Duration.ofMinutes(1))
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            study.studentEntry(student, studyClass, contest)
            study.submission(student.id, task, contest, "best.py", score = 90)
            study.submission(student.id, task, contest, "latest.py")
            signIn(student)
            open(studyClass, contest)

            assertTrue("90В очереди" in StudyPages.rowTexts("Последняя Посылка").single())
            StudyPages.clickRow("Последняя Посылка")

            assertEquals(StudentTaskView::class.java, currentView)
            assertFalse(UI.getCurrent()._get<Button> { text = "Отправить" }.isEnabled)
        }

        private fun open(studyClass: Class, contest: Contest) {
            UI.getCurrent().navigate(StudentContestView::class.java, studentContestParameters(studyClass.id, contest.id))
        }
    }

    @Nested
    inner class ParticipantTests {
        @Test
        fun `should lead the breadcrumbs to the cabinet`() {
            val contest = study.runningContest()
            signIn(study.participant(listOf(contest)))

            open(contest)

            assertEquals(mapOf("Кабинет Участника" to "participant"), StudyPages.crumbs())
        }

        @Test
        fun `should title the browser tab of the contest`() {
            val contest = study.runningContest()
            signIn(study.participant(listOf(contest)))

            open(contest)

            assertEquals("Тур", assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
        }

        @Test
        fun `should keep the start disabled after the end of the contest`() {
            val contest = study.contest(startsAt = StudyFixtures.PAST, contestDuration = Duration.ofHours(1))
            signIn(study.participant(listOf(contest)))

            open(contest)

            assertFalse(startAction().isEnabled)
        }

        @Test
        fun `should save the first entry after the confirmation`() {
            val contest = study.runningContest()
            val participant = study.participant(listOf(contest))
            signIn(participant)
            open(contest)

            startAction()._click()
            UI.getCurrent()._get<Button> { text = "Начать" }._click()

            val entry = participantEntries.findByContext(
                participantId = participant.id,
                competitionId = participant.data.competition.id,
                contestId = contest.id,
            )
            assertNotNull(entry)
            assertEquals("Тур начат", lastToastTitle())
        }

        @Test
        fun `should open a task after the first entry`() {
            val contest = study.runningContest(tasks = listOf(study.task()))
            val participant = study.participant(listOf(contest))
            study.participantEntry(participant, contest)
            signIn(participant)
            open(contest)

            StudyPages.clickRow("Название")

            assertEquals(ParticipantTaskView::class.java, currentView)
        }

        private fun open(contest: Contest) {
            val parameters = RouteParameters(STUDY_CONTEST_ID_PARAMETER, contest.id.value.toString())
            UI.getCurrent().navigate(ParticipantContestView::class.java, parameters)
        }
    }

    private fun startAction(): Button = UI.getCurrent()._get<Button> { text = "Начать Тур" }
}
