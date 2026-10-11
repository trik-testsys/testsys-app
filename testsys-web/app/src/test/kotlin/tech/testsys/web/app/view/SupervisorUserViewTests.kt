package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.Student
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.service.CommunityVo

@SpringBootTest
class SupervisorUserViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    @Test
    fun `should show an observer for viewing without modification actions`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val accessToken = fixtures.unique("observer-code")
        val observer = fixtures.observer(fixtures.community(), rawAccessToken = accessToken)

        open(observer.id.value)

        assertEquals(SupervisorUserView::class.java, currentView)
        assertEquals(accessToken, field("Код-доступа").value)
        assertTrue(field("Код-доступа").isReadOnly)
        assertTrue(UI.getCurrent()._find<Button> { text = "Включить в сообщество" }.isEmpty())
        assertTrue(UI.getCurrent()._find<Button> { text = "Удалить наблюдателя" }.isEmpty())
    }

    @Test
    fun `should add a user to a foreign community in a newly granted role`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val original = fixtures.community()
        val target = fixtures.community(name = "Целевое сообщество")
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(original.id.value))
                    data = developerData {}
                }
            }
        }
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Включить в сообщество" }._click()
        choose("Сообщество", CommunityVo::id, target.id)
        choose("Роль", { role: CommunityRole -> role }, CommunityRole.Student)
        UI.getCurrent()._get<Button> { text = "Включить" }._click()

        val updated = checkNotNull(users.findById(member.id))
        val student = updated.data.roles.filterIsInstance<Student>().single()
        assertTrue(target.id in student.memberOf.ids)
        assertTrue(original.id in updated.data.roles.first { it !is Student }.memberOf.ids)
        assertEquals(SupervisorUserView::class.java, currentView)
    }

    @Test
    fun `should refuse a supervisor detail route`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)

        open(supervisor.id.value)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    private fun open(id: Long) {
        UI.getCurrent().navigate(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
    }

    private fun field(label: String): TextField = UI.getCurrent()._find<TextField>().single { it.ariaLabel.orElse(null) == label }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, K> choose(label: String, key: (T) -> K, expected: K) {
        val select = UI.getCurrent()._get<Dialog>()._find<Select<*>>().single { it.ariaLabel.orElse(null) == label }
        val typed = checkNotNull(select as? Select<T?>)
        typed.value = typed.listDataView.items.toList().filterNotNull().single { item -> key(item) == expected }
    }
}
