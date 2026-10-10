package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.ContestId
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.toVo
import javax.sql.DataSource

@SpringBootTest
class DeveloperContestViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    private val developers by lazy { DeveloperFixtures(fixtures, developerService, tasks, dataSource) }

    @Test
    fun `should show the forbidden screen for an unshared contest of another developer`() {
        developers.signInDeveloper()
        val contest = developers.contest(developers.trikStudioVersion())
        developers.signInDeveloper()

        open(contest.id)

        assertEquals(OperationErrorView::class.java, currentView)
    }

    @Test
    fun `should save the edited name of the contest`() {
        developers.signInDeveloper()
        val contest = developers.contest(developers.trikStudioVersion(), name = "Весенний тур")
        open(contest.id)

        clickButton("Изменить")
        textField("Название")._value = "Летний тур"
        clickButton("Сохранить")

        assertEquals("Летний тур", developerService.viewContest(contest.id).first.name)
        assertEquals("Тур «Летний тур»", UI.getCurrent()._get<H1>().text)
        assertEquals("Тур изменён", lastToastTitle())
    }

    @Test
    fun `should attach a committed task and show it in the tasks of the contest`() {
        developers.signInDeveloper()
        val version = developers.trikStudioVersion()
        val taskId = developers.committedTask(version, name = "Движение по линии")
        val contest = developers.contest(version)
        open(contest.id)

        clickButton("Прикрепить задачу")
        taskField().value = developerService.viewTask(taskId).first
        clickButton("Прикрепить")

        assertEquals(listOf(taskId), developerService.viewContest(contest.id).first.tasks)
        assertTrue("Движение по линии" in tableText("Состояние"), tableText("Состояние"))
        assertEquals("Задача прикреплена к туру", lastToastTitle())
    }

    @Test
    fun `should detach a task from its menu`() {
        developers.signInDeveloper()
        val version = developers.trikStudioVersion()
        val taskId = developers.committedTask(version, name = "Движение по линии")
        val contest = developers.contest(version)
        developerService.attachTask(contest.id, taskId)
        open(contest.id)

        rowMenu("Действия с задачей «Движение по линии»")._clickItemWithCaption("Открепить")

        assertEquals(emptyList<Any>(), developerService.viewContest(contest.id).first.tasks)
        assertEquals("Задача откреплена от тура", lastToastTitle())
    }

    @Test
    fun `should share the contest after the confirmation that it can no longer be changed`() {
        val community = fixtures.community()
        developers.signInDeveloper(community)
        val contest = developers.contest(developers.trikStudioVersion())
        open(contest.id)

        clickButton("Предоставить доступ")
        communitiesField().value = setOf(community.toVo())
        clickButton("Предоставить")
        val warning = openDialog().element.textRecursively
        clickButton("Предоставить")

        assertTrue("нельзя будет изменить и удалить" in warning, warning)
        assertEquals(listOf(community.id), developerService.viewContest(contest.id).first.sharedTo)
        assertEquals("Доступ к туру предоставлен", lastToastTitle())
    }

    @Test
    fun `should block changes of a shared contest and explain why`() {
        val community = fixtures.community()
        developers.signInDeveloper(community)
        val contest = developers.contest(developers.trikStudioVersion())
        developerService.shareContest(contest.id, setOf(community.id))

        open(contest.id)

        assertTrue(alertDescriptions().single().startsWith("Доступ к туру предоставлен сообществам"), alertDescriptions().toString())
        assertEquals(emptyList<Button>(), UI.getCurrent()._find<Button> { text = "Изменить" })
        assertFalse(pageButton("Удалить тур").isEnabled)
        assertFalse(pageButton("Прикрепить задачу").isEnabled)
    }

    @Test
    fun `should explain that only the owner changes a contest shared to the developer`() {
        val community = fixtures.community()
        developers.signInDeveloper(community)
        val contest = developers.contest(developers.trikStudioVersion())
        developerService.shareContest(contest.id, setOf(community.id))
        developers.signInDeveloper(community)

        open(contest.id)

        assertEquals(listOf("Изменять тур может только его владелец."), alertDescriptions())
        assertFalse(pageButton("Предоставить доступ").isEnabled)
    }

    @Test
    fun `should delete the contest after the confirmation and open the contests`() {
        developers.signInDeveloper()
        val contest = developers.contest(developers.trikStudioVersion(), name = "Весенний тур")
        open(contest.id)

        clickButton("Удалить тур")
        assertFalse(openDialog()._get<Button> { text = "Удалить" }.isEnabled)
        openDialog()._get<TextField>()._setValue(contest.name)
        clickButton("Удалить")

        assertEquals("developer/contests", currentPath())
        assertEquals(0L, developerService.viewContests(Pagination(page = 0, size = 10)).totalElements)
        assertEquals("Тур удалён", lastToastTitle())
    }

    @Test
    fun `should explain an uncommitted task refusal and keep the attachment dialog open`() {
        developers.signInDeveloper()
        val task = developers.task()
        val contest = developers.contest(developers.trikStudioVersion())
        open(contest.id)
        clickButton("Прикрепить задачу")
        taskField().value = task

        clickButton("Прикрепить")

        assertTrue(openDialog().isOpened)
        assertEquals("Не удалось прикрепить задачу", lastToastTitle())
        assertEquals("У задачи нет зафиксированной версии. Сначала протестируйте и зафиксируйте задачу.", lastToastDescription())
        assertEquals(emptyList<Any>(), developerService.viewContest(contest.id).first.tasks)
    }

    private fun open(contestId: ContestId) {
        UI.getCurrent().navigate(DeveloperContestView::class.java, contestParameters(contestId))
    }

    /** Returns the task field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun taskField(): CustomField<TaskVo?> = checkNotNull(openDialog()._get<CustomField<*>>() as? CustomField<TaskVo?>)

    /** Returns the communities field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun communitiesField(): CustomField<Set<CommunityVo>> =
        checkNotNull(openDialog()._get<CustomField<*>>() as? CustomField<Set<CommunityVo>>)
}
