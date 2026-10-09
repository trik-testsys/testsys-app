package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.manager.ManagerService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
class ManagerClassViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var classes: ClassRepository

    @Autowired
    private lateinit var invites: ClassInviteRepository

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var managerService: ManagerService

    @Test
    fun `should show the not found screen for a missing class`() {
        signInManager()

        open(ClassId(Long.MAX_VALUE))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a class of another manager`() {
        signInManager()

        open(fixtures.studyClass(owner = fixtures.multipleRoleUser()).id)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should lead the breadcrumbs to the cabinet and its classes`() {
        open(fixtures.studyClass(owner = signInManager(), name = "Кружок").id)

        val crumbs = UI.getCurrent()._get<Nav> { classes = "ts-crumbs" }._find<RouterLink>().associate { link -> link.text to link.href }
        assertEquals(mapOf("Главная" to "home", "Кабинет Организатора" to "manager", "Классы" to "manager/classes"), crumbs)
        assertEquals("Класс «Кружок»", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the students of the class with their last login`() {
        val student = fixtures.multipleRoleUser(name = "Ученик Иван")
        users.recordLogin(student.id, LocalDateTime.of(2026, 3, 14, 9, 30).atZone(ZoneId.systemDefault()).toInstant())

        open(fixtures.studyClass(owner = signInManager(), students = listOf(student)).id)

        val text = tableText("Псевдоним")
        assertTrue("Ученик Иван" in text, text)
        assertTrue("14.03.2026 09:30" in text, text)
    }

    @Test
    fun `should show the invite code of the class in a hidden field`() {
        val studyClass = fixtures.studyClass(owner = signInManager())

        open(studyClass.id)

        val field = codeField()
        assertEquals(invite(studyClass).codeHash.value, field.value)
        val ancestors = generateSequence(field.parent.orElse(null)) { component -> component.parent.orElse(null) }
        assertTrue(ancestors.any { component -> component.element.hasAttribute("data-ts-obscured") })
    }

    @Test
    fun `should copy the invite code to the clipboard`() {
        val studyClass = fixtures.studyClass(owner = signInManager())
        open(studyClass.id)
        pendingJavaScript()

        UI.getCurrent()._get<Button> { text = "Скопировать" }._click()

        val copied = pendingJavaScript().filter { call -> "navigator.clipboard.writeText" in call.invocation.expression }
        assertEquals(listOf(invite(studyClass).codeHash.value), copied.map { call -> call.invocation.parameters.first() })
    }

    @Test
    fun `should replace the invite code after the confirmation`() {
        val studyClass = fixtures.studyClass(owner = signInManager())
        val before = invite(studyClass).codeHash
        open(studyClass.id)

        inviteMenu()._clickItemWithCaption("Заменить")
        UI.getCurrent()._get<Button> { text = "Заменить" }._click()

        val after = invite(studyClass).codeHash
        assertNotEquals(before, after)
        assertEquals(after.value, codeField().value)
    }

    @Test
    fun `should extend the invite code without changing it`() {
        val studyClass = fixtures.studyClass(owner = signInManager())
        val before = invite(studyClass)
        open(studyClass.id)

        inviteMenu()._clickItemWithCaption("Продлить")

        val after = invite(studyClass)
        assertEquals(before.codeHash, after.codeHash)
        assertTrue(after.expiresAt < before.expiresAt)
        assertTrue(after.expiresAt > Instant.now())
    }

    @Test
    fun `should add a contest shared to a community of the manager and show it`() {
        val community = fixtures.community()
        val manager = fixtures.grantRole(fixtures.multipleRoleUser(), CommunityRole.Manager, community).also { manager -> signIn(manager) }
        fixtures.contest(name = "Весенний тур", sharedTo = listOf(community))
        val studyClass = fixtures.studyClass(owner = manager)
        open(studyClass.id)
        val contest = managerService.viewAvailableContests(Pagination(page = 0, size = 1)).content.single()

        UI.getCurrent()._get<Button> { text = "Добавить тур" }._click()
        contestField().value = contest
        UI.getCurrent()._get<Button> { text = "Добавить" }._click()

        assertEquals(listOf(contest.id), checkNotNull(classes.findById(studyClass.id)).data.contests.ids)
        assertTrue("Весенний тур" in tableText("Время на прохождение"))
    }

    @Test
    fun `should open the results of a contest of the class from its row`() {
        val contest = fixtures.contest()
        val studyClass = fixtures.studyClass(owner = signInManager(), contests = listOf(contest))
        open(studyClass.id)

        clickRow(table("Время на прохождение")._get<TableBody>()._get<TableRow>())

        assertEquals(ManagerContestView::class.java, currentView)
        val path = UI.getCurrent().internals.activeViewLocation.path
        assertEquals("manager/classes/${studyClass.id.value}/contests/${contest.id.value}", path)
    }

    private fun signInManager(): MultipleRoleUser =
        fixtures.grantRole(fixtures.multipleRoleUser(), CommunityRole.Manager).also { manager -> signIn(manager) }

    private fun open(classId: ClassId) {
        UI.getCurrent().navigate(ManagerClassView::class.java, classParameters(classId))
    }

    private fun invite(studyClass: Class): ClassInviteData = checkNotNull(invites.findById(studyClass.data.invite.id)).data

    private fun codeField(): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == "Код-приглашение" }

    private fun inviteMenu(): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button -> button.element.getAttribute("aria-label") == "Действия" }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    private fun table(header: String): Table = UI.getCurrent()._find<Table>().single { table -> header in table.element.textRecursively }

    private fun tableText(header: String): String = table(header).element.textRecursively

    /** Returns the contest field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun contestField(): CustomField<ContestVo?> =
        checkNotNull(UI.getCurrent()._get<Dialog>()._get<CustomField<*>>() as? CustomField<ContestVo?>)

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }
}
