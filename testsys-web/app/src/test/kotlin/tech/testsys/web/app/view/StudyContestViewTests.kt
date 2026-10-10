package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import java.time.Duration

@SpringBootTest
class StudyContestViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var study: StudyFixtures

    @Autowired
    private lateinit var studentEntries: StudentContestEntryRepository

    @Autowired
    private lateinit var participantEntries: ParticipantContestEntryRepository

    @Nested
    inner class StudentTests {
        @Test
        fun `should lead the breadcrumbs to the main page, the cabinet and the class`() {
            val student = study.student()
            val contest = study.runningContest(name = "Весенний тур")
            val studyClass = study.studentClass(listOf(student), listOf(contest), name = "Кружок")
            signIn(student)

            open(studyClass, contest)

            assertEquals("Тур «Весенний тур»", UI.getCurrent()._get<H1>().text)
            assertEquals(
                mapOf("Главная" to "home", "Кабинет Ученика" to "student", "Класс «Кружок»" to "student/classes/${studyClass.id.value}"),
                StudyPages.crumbs(),
            )
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
            assertTrue(UI.getCurrent()._find<Button> { text = "Начать тур" }.isEmpty())
            assertEquals("Тур начат", lastToastTitle())
        }

        @Test
        fun `should not open a task before the first entry`() {
            val student = study.student()
            val contest = study.runningContest(tasks = listOf(study.task()))
            val studyClass = study.studentClass(listOf(student), listOf(contest))
            signIn(student)
            open(studyClass, contest)

            StudyPages.clickRow("Название")

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

    private fun startAction(): Button = UI.getCurrent()._get<Button> { text = "Начать тур" }
}
