package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Observer
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.SERVICE_TEST_USERS
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.error.ServiceAccessDeniedView
import tech.testsys.web.service.error.ServiceNotFoundView
import tech.testsys.web.service.security.ServiceSignIn
import tech.testsys.web.service.stubServiceTestUsers

class ServiceUserViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var search: ServiceUserSearch

    @Autowired
    private lateinit var signIn: ServiceSignIn

    private val observer = SERVICE_TEST_USERS.filterIsInstance<Observer>().single()

    private val observerId = ServiceUserId(observer.id.value)

    @BeforeEach
    fun prepareSearch() {
        clearMocks(search)
        stubServiceTestUsers(search)
        signIn.signIn(SERVICE_TEST_SECRET)
    }

    @Test
    fun `should deny the card to a main cabinet principal without service authority`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            CabinetPrincipal(userId = 1, kind = UserKind.MULTIPLE_ROLE),
            null,
            listOf(SimpleGrantedAuthority("ROLE_MULTIPLE_ROLE")),
        )

        open(1001)

        assertEquals(ServiceAccessDeniedView::class.java, currentView)
    }

    @ParameterizedTest
    @ValueSource(strings = ["999", "abc", "99999999999999999999"])
    fun `should show the not found screen for a missing or malformed user id`(userId: String) {
        UI.getCurrent().navigate(ServiceUserView::class.java, RouteParameters(SERVICE_USER_ID_PARAMETER, userId))

        assertEquals(ServiceNotFoundView::class.java, currentView)
    }

    @Nested
    inner class DetailsTests {
        @Test
        fun `should show the read-only data of a user with non-fixed roles`() {
            open(1001)

            assertEquals("1001", textField("ID").value)
            assertEquals("Мария Иванова", textField("Псевдоним").value)
            assertEquals("maria@example.com", textField("Почта").value)
            assertEquals(listOf("Администратор", "Ученик"), tags())
            assertTrue(listOf("ID", "Псевдоним", "Почта").all { label -> textField(label).isReadOnly })
            assertTrue(UI.getCurrent()._get<DateTimePicker>().isReadOnly)
        }

        @Test
        fun `should show the competition of a participant without an e-mail`() {
            open(1002)

            assertEquals("2001", textField("Соревнование (ID)").value)
            assertEquals(listOf("Участник"), tags())
            assertEquals(emptyList<TextField>(), fields("Почта"))
        }

        @Test
        fun `should show the community and the assigned contests of an observer`() {
            open(1003)

            assertEquals("3001", textField("Сообщество (ID)").value)
            assertEquals("4001, 4002", textField("Туры (ID)").value)
            assertEquals(listOf("Наблюдатель"), tags())
        }

        @Test
        fun `should show only the common data of a supervisor`() {
            open(1004)

            assertEquals("Супервайзер Олег", textField("Псевдоним").value)
            assertEquals(listOf("Супервайзер"), tags())
            assertEquals(emptyList<TextField>(), fields("Почта") + fields("Сообщество (ID)") + fields("Соревнование (ID)"))
        }
    }

    @Nested
    inner class AccessTokenTests {
        @ParameterizedTest
        @CsvSource("1001, multiple-role-token", "1002, participant-token", "1003, observer-token", "1004, supervisor-token")
        fun `should show the access code of each user kind in a hidden read-only field`(userId: Long, accessToken: String) {
            open(userId)

            val field = textField("Код-доступа")
            val ancestors = generateSequence(field.parent.orElse(null)) { component -> component.parent.orElse(null) }
            assertEquals(accessToken, field.value)
            assertTrue(field.isReadOnly)
            assertTrue(ancestors.any { component -> component.element.hasAttribute("data-ts-obscured") })
            assertEquals(listOf("Копировать"), accessTokenActions())
        }

        @Test
        fun `should copy the access code shown in the field without reading the user again`() {
            open(1003)
            every { search.findById(observerId) } returns observerWithAccessToken("observer-token-2")
            pendingJavaScript()

            UI.getCurrent()._get<Button> { text = "Копировать" }._click()

            val copied = pendingJavaScript().filter { call -> "navigator.clipboard.writeText" in call.invocation.expression }
            assertEquals(listOf("observer-token"), copied.map { call -> call.invocation.parameters.first() })
            verify(exactly = 1) { search.findById(observerId) }
        }

        @Test
        fun `should keep the access code out of the address`() {
            open(1003)

            assertEquals("users/1003", UI.getCurrent().internals.activeViewLocation.pathWithQueryParameters)
        }
    }

    private fun open(userId: Long) {
        UI.getCurrent().navigate(ServiceUserView::class.java, RouteParameters(SERVICE_USER_ID_PARAMETER, userId.toString()))
    }

    private fun observerWithAccessToken(accessToken: String): Observer =
        observer.withData { accessToken(accessToken, algorithm = HashAlgorithm.Identity) }

    private fun fields(label: String): List<TextField> =
        UI.getCurrent()._find<TextField>().filter { field -> field.ariaLabel.orElse(null) == label }

    private fun textField(label: String): TextField = fields(label).single()

    private fun tags(): List<String> = UI.getCurrent()._find<Span> { classes = "ts-tag" }.map { tag -> tag.text }

    private fun accessTokenActions(): List<String> = UI.getCurrent()._find<Button>()
        .map { button -> button.text }
        .filter { text -> text in setOf("Показать", "Скрыть", "Копировать") }

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }
}
