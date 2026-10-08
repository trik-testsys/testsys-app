package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.CommunityVo
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
class AdminUserViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var users: UserRepository

    @Test
    fun `should show the common data and the roles of a member in the communities of the administrator`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = developerOf(community, name = "Разработчик Иван")

        openUser(member)

        assertEquals(AdminUserView::class.java, currentView)
        assertEquals(member.id.value.toString(), textField("ID").value)
        assertEquals("Разработчик Иван", textField("Псевдоним").value)
        assertTrue("КружокРазработчик" in rolesText(), rolesText())
    }

    @Test
    fun `should show all roles of a member in one community in one row`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(community.id.value))
                    data = developerData {}
                }
                student {
                    memberOf(listOf(community.id.value))
                    data = studentData {}
                }
            }
        }

        openUser(member)

        assertTrue("КружокРазработчик, Ученик" in rolesText(), rolesText())
    }

    @Test
    fun `should show the details in read-only form fields`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))

        openUser(member)

        assertTrue(textField("ID").isReadOnly)
        assertTrue(textField("Псевдоним").isReadOnly)
        assertTrue(UI.getCurrent()._get<DateTimePicker>().isReadOnly)
    }

    @Test
    fun `should show the access code of an observer at its own address without role granting`() {
        val community = fixtures.community(owner = signInAdministrator())
        val observer = fixtures.observer(community, rawAccessToken = "observer-code")

        UI.getCurrent().navigate(AdminUserView::class.java, RouteParameters(OBSERVER_ID_PARAMETER, observer.id.value.toString()))

        val accessToken = textField("Код-доступа")
        assertEquals("observer-code", accessToken.value)
        assertTrue(accessToken.isReadOnly)
        assertEquals(emptyList<Button>(), UI.getCurrent()._find<Button> { text = "Включить в сообщество" })
    }

    @Test
    fun `should show the not found screen for a missing user`() {
        signInAdministrator()

        UI.getCurrent().navigate(AdminUserView::class.java, RouteParameters(USER_ID_PARAMETER, Long.MAX_VALUE.toString()))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a user of another administrator`() {
        signInAdministrator()
        val stranger = developerOf(fixtures.community())

        openUser(stranger)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should grant the chosen role in a community of the administrator and show it`() {
        val administrator = signInAdministrator()
        val member = developerOf(fixtures.community(owner = administrator))
        val other = fixtures.community(owner = administrator, name = "Второе сообщество")
        openUser(member)

        UI.getCurrent()._get<Button> { text = "Включить в сообщество" }._click()
        choose("Сообщество", CommunityVo::id, other.id)
        choose("Роль", { role: CommunityRole -> role }, CommunityRole.Student)
        UI.getCurrent()._get<Button> { text = "Включить" }._click()

        val student = checkNotNull(multipleRoleUsers.findById(member.id)).data.roles.filterIsInstance<Student>().single()
        assertEquals(listOf(other.id), student.memberOf.ids)
        assertTrue("Второе сообществоУченик" in rolesText(), rolesText())
    }

    @Test
    fun `should show the date and time of the last login in the details`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))
        users.recordLogin(member.id, LocalDateTime.of(2026, 3, 14, 9, 30).atZone(ZoneId.systemDefault()).toInstant())

        openUser(member)

        assertEquals(LocalDateTime.of(2026, 3, 14, 9, 30), UI.getCurrent()._get<DateTimePicker>().value)
    }

    @Test
    fun `should leave the last login empty for a user who has never signed in`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))

        openUser(member)

        assertNull(UI.getCurrent()._get<DateTimePicker>().value)
    }

    private fun signInAdministrator(): MultipleRoleUser = fixtures.administrator().also { administrator -> signIn(administrator) }

    private fun developerOf(community: Community, name: String = fixtures.unique("Developer")): MultipleRoleUser =
        fixtures.multipleRoleUser(name = name) {
            roles {
                developer {
                    memberOf(listOf(community.id.value))
                    data = developerData {}
                }
            }
        }

    private fun openUser(user: MultipleRoleUser) {
        UI.getCurrent().navigate(AdminUserView::class.java, RouteParameters(USER_ID_PARAMETER, user.id.value.toString()))
    }

    private fun rolesText(): String = UI.getCurrent()._find<Table>().single().element.textRecursively

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    /** Chooses the item of the select [label] of the open dialog whose [key] is [expected]. */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, K> choose(label: String, key: (T) -> K, expected: K) {
        val select = UI.getCurrent()._get<Dialog>()._find<Select<*>>().single { candidate -> candidate.ariaLabel.orElse(null) == label }
        val typed = checkNotNull(select as? Select<T?>)
        typed.value = typed.listDataView.items.toList().filterNotNull().single { item -> key(item) == expected }
    }
}
