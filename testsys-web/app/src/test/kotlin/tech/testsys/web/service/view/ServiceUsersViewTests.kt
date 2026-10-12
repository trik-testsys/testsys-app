package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.infra.database.api.service.ServiceUserFilter
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserRole
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.view.clickRow
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.SERVICE_TEST_USERS
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.error.ServiceAccessDeniedView
import tech.testsys.web.service.security.ServiceSignIn
import tech.testsys.web.service.stubServiceTestUsers

class ServiceUsersViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var search: ServiceUserSearch

    @Autowired
    private lateinit var signIn: ServiceSignIn

    private val firstPage = Pagination(page = 0, size = 20)

    @BeforeEach
    fun prepareSearch() {
        clearMocks(search)
        stubServiceTestUsers(search)
        every { search.find(any(), any()) } answers { Page(SERVICE_TEST_USERS, secondArg(), SERVICE_TEST_USERS.size.toLong()) }
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceUsersView::class.java)
    }

    @Test
    fun `should deny the user search to a main cabinet principal without service authority`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            CabinetPrincipal(userId = 1, kind = UserKind.MULTIPLE_ROLE),
            null,
            listOf(SimpleGrantedAuthority("ROLE_MULTIPLE_ROLE")),
        )

        UI.getCurrent().navigate(ServiceUsersView::class.java)

        assertEquals(ServiceAccessDeniedView::class.java, currentView)
    }

    @Test
    fun `should open the user search from the service home`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        UI.getCurrent()._get<Button> { text = "Пользователи" }._click()

        assertEquals(ServiceUsersView::class.java, currentView)
    }

    @Test
    fun `should search all users on the first page when opened`() {
        verify { search.find(ServiceUserFilter(), firstPage) }
    }

    @Test
    fun `should show each role of users of every kind as a separate tag`() {
        val tags = rows().map { row -> row._find<Span> { classes = "ts-tag" }.map { tag -> tag.text } }

        assertEquals(listOf(listOf("Администратор", "Ученик"), listOf("Участник"), listOf("Наблюдатель"), listOf("Супервайзер")), tags)
    }

    @Test
    fun `should show the e-mail only of a user with non-fixed roles`() {
        val texts = rows().map { row -> row.element.textRecursively }

        assertTrue("maria@example.com" in texts.first(), texts.first())
        assertTrue(texts.drop(1).all { text -> "@" !in text }, texts.toString())
    }

    @Test
    fun `should search by every applied condition`() {
        textField("ID")._value = " 1001 "
        textField("Псевдоним")._value = "Мар"
        textField("Почта")._value = "Example"
        rolesFilter().value = setOf(ServiceUserRole.ADMINISTRATOR, ServiceUserRole.PARTICIPANT)
        textField("Сообщество (ID)")._value = "3001"
        textField("Класс (ID)")._value = "5001"
        textField("Соревнование (ID)")._value = "2001"

        apply()

        verify {
            search.find(
                ServiceUserFilter(
                    id = ServiceUserId(1001),
                    name = "Мар",
                    email = "Example",
                    roles = setOf(ServiceUserRole.ADMINISTRATOR, ServiceUserRole.PARTICIPANT),
                    communityId = CommunityId(3001),
                    classId = ClassId(5001),
                    competitionId = CompetitionId(2001),
                ),
                firstPage,
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["ID", "Сообщество (ID)", "Класс (ID)", "Соревнование (ID)"])
    fun `should mark a non-numeric identifier invalid and keep the search`(label: String) {
        textField(label)._value = "12a"

        apply()

        assertTrue(textField(label).isInvalid)
        assertEquals("Введите число", textField(label).errorMessage)
        verify(exactly = 1) { search.find(any(), any()) }
    }

    @Test
    fun `should clear the conditions and search all users after the reset`() {
        textField("Псевдоним")._value = "Мар"
        textField("ID")._value = "12a"
        apply()

        UI.getCurrent()._get<Button> { text = "Сбросить" }._click()

        assertEquals("", textField("Псевдоним").value)
        assertFalse(textField("ID").isInvalid)
        verify(exactly = 2) { search.find(ServiceUserFilter(), firstPage) }
    }

    @Test
    fun `should open the card of the clicked user by its id`() {
        clickRow(rows()[2])

        assertEquals(ServiceUserView::class.java, currentView)
        assertEquals("users/1003", UI.getCurrent().internals.activeViewLocation.path)
    }

    private fun rows(): List<TableRow> = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>()

    private fun apply() {
        UI.getCurrent()._get<Button> { text = "Применить" }._click()
    }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    /** Returns the role filter, the multi-selection named «Роли». */
    @Suppress("UNCHECKED_CAST")
    private fun rolesFilter(): CustomField<Set<ServiceUserRole>> {
        val field = UI.getCurrent()._find<CustomField<*>>().single { candidate ->
            candidate.element.getProperty("accessibleName") == "Роли"
        }
        return checkNotNull(field as? CustomField<Set<ServiceUserRole>>)
    }
}
