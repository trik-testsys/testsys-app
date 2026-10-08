package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.contract.persistence.UserFilter
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.service.CommunityVo
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
class AdminViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var communities: CommunityRepository

    @Autowired
    private lateinit var users: UserRepository

    @Test
    fun `should open the communities tab when no section is given`() {
        signIn(fixtures.administrator())

        UI.getCurrent().navigate(AdminView::class.java)

        assertEquals(AdminView::class.java, currentView)
        assertEquals("admin/communities", UI.getCurrent().internals.activeViewLocation.path)
    }

    @Test
    fun `should show the forbidden screen to a user without the administrator role`() {
        signIn(fixtures.userOf(UserKind.MULTIPLE_ROLE))

        UI.getCurrent().navigate(AdminView::class.java, section("communities"))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the communities of the administrator with the number of their users`() {
        val administrator = signInAdministrator()
        val community = fixtures.community(owner = administrator, name = "Кружок робототехники")
        fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(community.id.value))
                    data = developerData {}
                }
            }
        }
        fixtures.community(name = "Чужое сообщество")

        UI.getCurrent().navigate(AdminView::class.java, section("communities"))

        val text = tableText()
        assertTrue("Кружок робототехники" in text, text)
        assertTrue("Чужое сообщество" !in text, text)
        assertTrue(text.endsWith("2"), text)
    }

    @Test
    fun `should create a community from the dialog and show it in the table`() {
        val administrator = signInAdministrator()
        UI.getCurrent().navigate(AdminView::class.java, section("communities"))

        UI.getCurrent()._get<Button> { text = "Создать сообщество" }._click()
        field("Название")._value = "Новое сообщество"
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        assertEquals(listOf("Новое сообщество"), communities.findByOwner(administrator.id).map { community -> community.data.name })
        assertTrue("Новое сообщество" in tableText())
    }

    @Test
    fun `should create nothing if the name of the community is blank`() {
        val administrator = signInAdministrator()
        UI.getCurrent().navigate(AdminView::class.java, section("communities"))

        UI.getCurrent()._get<Button> { text = "Создать сообщество" }._click()
        field("Название")._value = "   "
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        assertEquals(emptyList<Any>(), communities.findByOwner(administrator.id))
    }

    @Test
    fun `should list the users of the communities of the administrator on the users tab`() {
        val administrator = signInAdministrator()
        val community = fixtures.community(owner = administrator)
        fixtures.observer(community, name = "Наблюдатель Пётр")

        UI.getCurrent().navigate(AdminView::class.java, section("users"))

        val text = tableText()
        assertTrue(administrator.data.name in text, text)
        assertTrue("Наблюдатель Пётр" in text, text)
    }

    @Test
    fun `should show only the users of the chosen role after the filter is applied`() {
        val community = fixtures.community(owner = signInAdministrator())
        developerOf(community, name = "Разработчик Иван")
        fixtures.observer(community, name = "Наблюдатель Пётр")
        UI.getCurrent().navigate(AdminView::class.java, section("users"))

        rolesFilter().value = setOf(UserFilter.Role.OBSERVER)
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue("Наблюдатель Пётр" in text, text)
        assertTrue("Разработчик Иван" !in text, text)
    }

    @Test
    fun `should show only the users whose nickname contains the filter text after it is applied`() {
        val community = fixtures.community(owner = signInAdministrator())
        developerOf(community, name = "Анна Смирнова")
        developerOf(community, name = "Иван Петров")
        UI.getCurrent().navigate(AdminView::class.java, section("users"))

        field("Псевдоним")._value = "смирн"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue("Анна Смирнова" in text, text)
        assertTrue("Иван Петров" !in text, text)
    }

    @Test
    fun `should show only the users of the chosen community after the filter is applied`() {
        val administrator = signInAdministrator()
        val first = fixtures.community(owner = administrator)
        developerOf(first, name = "Разработчик Иван")
        developerOf(fixtures.community(owner = administrator), name = "Разработчик Олег")
        UI.getCurrent().navigate(AdminView::class.java, section("users"))
        val filter = communityFilter()

        filter.value = filter.listDataView.items.toList().single { community -> community?.id == first.id }
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue("Разработчик Иван" in text, text)
        assertTrue("Разработчик Олег" !in text, text)
    }

    @Test
    fun `should clear the filters and show all users when the filters are reset`() {
        val community = fixtures.community(owner = signInAdministrator())
        developerOf(community, name = "Анна Смирнова")
        developerOf(community, name = "Иван Петров")
        UI.getCurrent().navigate(AdminView::class.java, section("users"))
        field("Псевдоним")._value = "смирн"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        UI.getCurrent()._get<Button> { text = "Сбросить" }._click()

        val text = tableText()
        assertEquals("", field("Псевдоним").value)
        assertTrue("Анна Смирнова" in text, text)
        assertTrue("Иван Петров" in text, text)
    }

    @Test
    fun `should show the last login of a user in the users table`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()), name = "Разработчик Иван")
        users.recordLogin(member.id, LocalDateTime.of(2026, 3, 14, 9, 30).atZone(ZoneId.systemDefault()).toInstant())

        UI.getCurrent().navigate(AdminView::class.java, section("users"))

        val text = tableText()
        assertTrue("14.03.2026 09:30" in text, text)
    }

    @Test
    fun `should show the users table in the same page when the users tab is opened after the communities tab`() {
        val administrator = signInAdministrator()
        fixtures.observer(fixtures.community(owner = administrator), name = "Наблюдатель Пётр")
        UI.getCurrent().navigate(AdminView::class.java, section("communities"))
        val page = UI.getCurrent()._get<AdminView>()

        UI.getCurrent().navigate(AdminView::class.java, section("users"))

        assertSame(page, UI.getCurrent()._get<AdminView>())
        assertEquals(emptyList<Button>(), UI.getCurrent()._find<Button> { text = "Создать сообщество" })
        val shown = tableText()
        assertTrue("Наблюдатель Пётр" in shown, shown)
    }

    private fun signInAdministrator(): MultipleRoleUser = fixtures.administrator().also { administrator -> signIn(administrator) }

    private fun developerOf(community: Community, name: String): MultipleRoleUser = fixtures.multipleRoleUser(name = name) {
        roles {
            developer {
                memberOf(listOf(community.id.value))
                data = developerData {}
            }
        }
    }

    private fun section(name: String) = RouteParameters(ADMIN_SECTION_PARAMETER, name)

    private fun field(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    /** Returns the role filter, the multi-selection named «Роли». */
    @Suppress("UNCHECKED_CAST")
    private fun rolesFilter(): CustomField<Set<UserFilter.Role>> {
        val field = UI.getCurrent()._find<CustomField<*>>().single { candidate ->
            candidate.element.getProperty("accessibleName") == "Роли"
        }
        return checkNotNull(field as? CustomField<Set<UserFilter.Role>>)
    }

    /** Returns the community filter, the select named «Сообщество». */
    @Suppress("UNCHECKED_CAST")
    private fun communityFilter(): Select<CommunityVo?> {
        val select = UI.getCurrent()._find<Select<*>>().single { candidate -> candidate.ariaLabel.orElse(null) == "Сообщество" }
        return checkNotNull(select as? Select<CommunityVo?>)
    }

    private fun tableText(): String = UI.getCurrent()._find<Table>().single().element.textRecursively
}
