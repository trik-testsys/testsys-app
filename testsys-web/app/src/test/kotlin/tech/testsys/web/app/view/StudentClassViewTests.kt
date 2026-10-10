package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.textfield.TextArea
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.model.group.Class
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import tech.testsys.web.app.error.OperationErrorView
import java.time.Duration

@SpringBootTest
class StudentClassViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var study: StudyFixtures

    @Test
    fun `should show the forbidden screen to a student outside the class`() {
        val studyClass = study.studentClass(listOf(study.student()))
        signIn(study.student())

        open(studyClass)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should lead the breadcrumbs to the main page and the cabinet`() {
        val student = study.student()
        val studyClass = study.studentClass(listOf(student), name = "Кружок")
        signIn(student)

        open(studyClass)

        assertEquals("Класс «Кружок»", UI.getCurrent()._get<H1>().text)
        assertEquals(mapOf("Главная" to "home", "Кабинет Ученика" to "student"), StudyPages.crumbs())
    }

    @Test
    fun `should show the id, name and description of the class in read-only fields`() {
        val student = study.student()
        val studyClass = study.studentClass(listOf(student), name = "Кружок")
        signIn(student)

        open(studyClass)

        assertEquals(studyClass.id.value.toString(), StudyPages.textField("ID").value)
        assertEquals("Кружок", StudyPages.textField("Название").value)
        assertEquals("Описание класса", description().value)
        assertTrue(StudyPages.textField("Название").isReadOnly && description().isReadOnly)
    }

    @Test
    fun `should show the state of each contest for the student`() {
        val student = study.student()
        val future = study.contest(name = "Будущий тур", startsAt = StudyFixtures.FAR_FUTURE, contestDuration = Duration.ofHours(1))
        val running = study.runningContest(name = "Идущий тур")
        val ended = study.contest(name = "Прошедший тур", startsAt = StudyFixtures.PAST, contestDuration = Duration.ofHours(1))
        val studyClass = study.studentClass(listOf(student), listOf(future, running, ended))
        study.studentEntry(student, studyClass, running)
        signIn(student)

        open(studyClass)

        val rows = StudyPages.rowTexts("Состояние")
        assertTrue("Не начат" in rows.single { row -> "Будущий тур" in row }, rows.toString())
        assertTrue("Идёт" in rows.single { row -> "Идущий тур" in row }, rows.toString())
        assertTrue("Завершён" in rows.single { row -> "Прошедший тур" in row }, rows.toString())
    }

    @Test
    fun `should open the page of a contest from its row`() {
        val student = study.student()
        val studyClass = study.studentClass(listOf(student), listOf(study.runningContest()))
        signIn(student)
        open(studyClass)

        StudyPages.clickRow("Состояние")

        assertEquals(StudentContestView::class.java, currentView)
    }

    private fun open(studyClass: Class) {
        UI.getCurrent().navigate(StudentClassView::class.java, studentClassParameters(studyClass.id))
    }

    private fun description(): TextArea = UI.getCurrent()._find<TextArea>().single { field -> field.ariaLabel.orElse(null) == "Описание" }
}
