package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView

@SpringBootTest
class ManagerContestViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var tasks: TaskRepository

    @Test
    fun `should show the not found screen for a contest not added to the class`() {
        val studyClass = fixtures.studyClass(owner = signInManager())

        UI.getCurrent().navigate(ManagerContestView::class.java, classContestParameters(studyClass.id, fixtures.contest().id))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the not found screen for a missing contest`() {
        val studyClass = fixtures.studyClass(owner = signInManager())

        UI.getCurrent().navigate(ManagerContestView::class.java, classContestParameters(studyClass.id, ContestId(Long.MAX_VALUE)))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a contest of a competition of another manager`() {
        signInManager()
        val contest = fixtures.contest()
        val competition = fixtures.competition(owner = fixtures.multipleRoleUser(), contests = listOf(contest))

        UI.getCurrent().navigate(ManagerContestView::class.java, competitionContestParameters(competition.id, contest.id))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should lead the breadcrumbs through the class of the contest`() {
        val contest = fixtures.contest(name = "Весенний тур")
        val studyClass = fixtures.studyClass(owner = signInManager(), name = "Кружок", contests = listOf(contest))

        UI.getCurrent().navigate(ManagerContestView::class.java, classContestParameters(studyClass.id, contest.id))

        val crumbs = UI.getCurrent()._get<Nav> { classes = "ts-crumbs" }._find<RouterLink>().associate { link -> link.text to link.href }
        assertEquals(
            mapOf(
                "Главная" to "home",
                "Кабинет Организатора" to "manager",
                "Классы" to "manager/classes",
                "Класс «Кружок»" to "manager/classes/${studyClass.id.value}",
            ),
            crumbs,
        )
        assertEquals("Тур «Весенний тур»", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show a column for each task and a row for each student without submissions`() {
        val first = task("Движение")
        val second = task("Повороты")
        val contest = fixtures.contest(tasks = listOf(first.id, second.id))
        val student = fixtures.multipleRoleUser(name = "Ученик Иван")
        val studyClass = fixtures.studyClass(owner = signInManager(), students = listOf(student), contests = listOf(contest))

        UI.getCurrent().navigate(ManagerContestView::class.java, classContestParameters(studyClass.id, contest.id))

        val text = UI.getCurrent()._find<Table>().single().element.textRecursively
        assertTrue("Движение · ${first.id.value}" in text, text)
        assertTrue("Повороты · ${second.id.value}" in text, text)
        assertTrue("${student.id.value}Ученик Иван— · 0— · 0" in text, text)
    }

    @Test
    fun `should show the participants of a competition contest`() {
        val contest = fixtures.contest()
        val competition = fixtures.competition(owner = signInManager(), name = "Кубок", contests = listOf(contest))

        UI.getCurrent().navigate(ManagerContestView::class.java, competitionContestParameters(competition.id, contest.id))

        assertEquals(ManagerContestView::class.java, currentView)
        val crumbs = UI.getCurrent()._get<Nav> { classes = "ts-crumbs" }._find<RouterLink>().map { link -> link.text }
        assertEquals(listOf("Главная", "Кабинет Организатора", "Соревнования", "Соревнование «Кубок»"), crumbs)
    }

    private fun signInManager(): MultipleRoleUser =
        fixtures.grantRole(fixtures.multipleRoleUser(), CommunityRole.Manager).also { manager -> signIn(manager) }

    private fun task(name: String): Task {
        val ownerId = fixtures.multipleRoleUser().id
        return tasks.save(
            taskData {
                owner = ownerId
                this.name = name
                description = ""
                content.new {}
            },
        )
    }
}
