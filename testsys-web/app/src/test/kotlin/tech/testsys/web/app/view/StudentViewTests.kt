package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import tech.testsys.web.app.error.OperationErrorView

@SpringBootTest
class StudentViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var study: StudyFixtures

    @Autowired
    private lateinit var classes: ClassRepository

    @Autowired
    private lateinit var classInvites: ClassInviteRepository

    @Test
    fun `should show the forbidden screen to a user without the student role`() {
        signIn(fixtures.multipleRoleUser())

        UI.getCurrent().navigate(StudentView::class.java)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should start the breadcrumbs with the main page`() {
        signIn(study.student())

        UI.getCurrent().navigate(StudentView::class.java)

        assertEquals(mapOf("Главная" to "home"), StudyPages.crumbs())
    }

    @Test
    fun `should list the classes of the student with their names and descriptions`() {
        val student = study.student()
        study.studentClass(listOf(student), name = "Робототехника")
        study.studentClass(listOf(study.student()), name = "Чужой класс")
        signIn(student)

        UI.getCurrent().navigate(StudentView::class.java)

        val rows = StudyPages.rowTexts("Описание")
        assertEquals(1, rows.size, rows.toString())
        assertTrue("Робототехника" in rows.single() && "Описание класса" in rows.single(), rows.toString())
    }

    @Test
    fun `should open the page of a class from its row`() {
        val student = study.student()
        study.studentClass(listOf(student))
        signIn(student)
        UI.getCurrent().navigate(StudentView::class.java)

        StudyPages.clickRow("Описание")

        assertEquals(StudentClassView::class.java, currentView)
    }

    @Test
    fun `should join the class by the invite code and show it`() {
        val student = study.student()
        val studyClass = study.studentClass(emptyList(), name = "Новый класс")
        signIn(student)
        UI.getCurrent().navigate(StudentView::class.java)

        StudyPages.textField("Код-приглашение")._value = inviteCode(studyClass)
        UI.getCurrent()._get<Button> { text = "Присоединиться" }._click()

        assertEquals(listOf(student.id), checkNotNull(classes.findById(studyClass.id)).data.students.ids)
        assertEquals("Вы присоединились к классу «Новый класс»", StudyPages.lastToastTitle())
        assertTrue("Новый класс" in StudyPages.rowTexts("Описание").single())
    }

    @Test
    fun `should tell that the invite code is not valid`() {
        signIn(study.student())
        UI.getCurrent().navigate(StudentView::class.java)

        StudyPages.textField("Код-приглашение")._value = fixtures.unique("code")
        UI.getCurrent()._get<Button> { text = "Присоединиться" }._click()

        assertEquals("Код-приглашение недействителен", StudyPages.lastToastTitle())
    }

    @Test
    fun `should tell that the invite code has expired`() {
        val studyClass = study.studentClass(emptyList(), inviteExpiresAt = StudyFixtures.PAST)
        signIn(study.student())
        UI.getCurrent().navigate(StudentView::class.java)

        StudyPages.textField("Код-приглашение")._value = inviteCode(studyClass)
        UI.getCurrent()._get<Button> { text = "Присоединиться" }._click()

        assertEquals("Срок Кода-приглашения истёк", StudyPages.lastToastTitle())
    }

    private fun inviteCode(studyClass: Class): String = checkNotNull(classInvites.findById(studyClass.data.invite.id)).data.codeHash.value
}
