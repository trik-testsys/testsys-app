package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableCell
import com.vaadin.flow.component.html.TableRow
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
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.infra.database.api.service.ServiceSupervisorRole
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.database.api.service.SupervisorRoleChange
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

    @Autowired
    private lateinit var supervisorRole: ServiceSupervisorRole

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
        fun `should show the assigned contests of an observer`() {
            open(1003)

            assertEquals("4001, 4002", textField("Туры (ID)").value)
            assertEquals(listOf("Наблюдатель"), tags())
            assertEquals(emptyList<TextField>(), fields("Сообщество (ID)"))
        }

        @Test
        fun `should show a supervisor as a user with non-fixed roles`() {
            open(1004)

            assertEquals("Супервайзер Олег", textField("Псевдоним").value)
            assertEquals("oleg@example.com", textField("Почта").value)
            assertEquals(listOf("Супервайзер"), tags())
        }
    }

    @Nested
    inner class CommunitiesTests {
        @Test
        fun `should list the communities of a user with non-fixed roles by id with the roles in each`() {
            open(1001)

            assertEquals(
                listOf(
                    CommunityRow("3001", "Олимпиада 2026", listOf("Администратор", "Ученик")),
                    CommunityRow("3002", "Кружок робототехники", listOf("Администратор")),
                ),
                communityRows(),
            )
        }

        @Test
        fun `should list the observed community of an observer`() {
            open(1003)

            assertEquals(listOf(CommunityRow("3001", "Олимпиада 2026", listOf("Наблюдатель"))), communityRows())
        }

        @Test
        fun `should show that a user without memberships is in no community`() {
            open(1005)

            assertEquals(emptyList<CommunityRow>(), communityRows())
            assertEquals("Пользователь не состоит в Сообществах", block("Сообщества")._get<Span> { classes = "ts-empty__title" }.text)
        }

        @Test
        fun `should show no communities of a participant`() {
            open(1002)

            assertTrue(blocks("Сообщества").isEmpty())
        }
    }

    @Nested
    inner class OpenSupervisorTests {
        @ParameterizedTest
        @ValueSource(longs = [1001, 1002, 1003])
        fun `should open a user a supervisor can sign in as in the cabinet of a supervisor in the same tab`(userId: Long) {
            open(userId)
            pendingJavaScript()

            UI.getCurrent()._get<Button> { text = OPEN_SUPERVISOR }._click()

            val transitions = pendingJavaScript().filter { call -> "window.open" in call.invocation.expression }
            assertEquals(
                listOf(listOf("https://testsys.example.test/testsys/supervisor/users/$userId", "_self")),
                transitions.map { call -> call.invocation.parameters },
            )
        }

        @ParameterizedTest
        @ValueSource(longs = [1004, 1005])
        fun `should not offer opening a supervisor or the owner of the public community`(userId: Long) {
            open(userId)

            assertTrue(UI.getCurrent()._find<Button> { text = OPEN_SUPERVISOR }.isEmpty())
        }
    }

    @Nested
    inner class SupervisorRoleTests {
        @BeforeEach
        fun resetRole() = clearMocks(supervisorRole)

        @Test
        fun `should offer granting the role to a user with non-fixed roles without it`() {
            open(1001)

            assertEquals(listOf("Выдать Роль Супервайзера"), roleActions())
        }

        @Test
        fun `should offer revoking the role of a supervisor`() {
            open(1004)

            assertEquals(listOf("Снять Роль Супервайзера"), roleActions())
        }

        @ParameterizedTest
        @ValueSource(longs = [1002, 1003, 1005])
        fun `should offer no role action to a fixed-role user or the owner of the public community`(userId: Long) {
            open(userId)

            assertEquals(emptyList<String>(), roleActions())
        }

        @Test
        fun `should grant the role after confirmation and show the updated card`() {
            val granted = SERVICE_TEST_USERS.filterIsInstance<MultipleRoleUser>().single { user -> user.id.value == 1004L }
            every { supervisorRole.grant(ServiceUserId(1001), PUBLIC_COMMUNITY_ID) } returns SupervisorRoleChange.Changed(granted)
            open(1001)

            UI.getCurrent()._get<Button> { text = "Выдать Роль Супервайзера" }._click()
            openDialog()._get<Button> { text = "Выдать" }._click()

            verify(exactly = 1) { supervisorRole.grant(ServiceUserId(1001), PUBLIC_COMMUNITY_ID) }
            assertEquals("Роль Супервайзера выдана", lastToastTitle())
            assertEquals(listOf("Снять Роль Супервайзера"), roleActions())
        }

        @Test
        fun `should revoke the role after confirmation`() {
            val revoked = SERVICE_TEST_USERS.filterIsInstance<MultipleRoleUser>().single { user -> user.id.value == 1001L }
            every { supervisorRole.revoke(ServiceUserId(1004), PUBLIC_COMMUNITY_ID) } returns SupervisorRoleChange.Changed(revoked)
            open(1004)

            UI.getCurrent()._get<Button> { text = "Снять Роль Супервайзера" }._click()
            openDialog()._get<Button> { text = "Снять" }._click()

            verify(exactly = 1) { supervisorRole.revoke(ServiceUserId(1004), PUBLIC_COMMUNITY_ID) }
            assertEquals("Роль Супервайзера снята", lastToastTitle())
        }

        @Test
        fun `should change nothing when the confirmation is cancelled`() {
            open(1001)

            UI.getCurrent()._get<Button> { text = "Выдать Роль Супервайзера" }._click()
            openDialog()._get<Button> { text = "Отменить" }._click()

            verify(exactly = 0) { supervisorRole.grant(any(), any()) }
        }

        @Test
        fun `should report a refusal without changing the card`() {
            every { supervisorRole.grant(ServiceUserId(1001), PUBLIC_COMMUNITY_ID) } returns SupervisorRoleChange.NotAllowed
            open(1001)

            UI.getCurrent()._get<Button> { text = "Выдать Роль Супервайзера" }._click()
            openDialog()._get<Button> { text = "Выдать" }._click()

            assertEquals("Этому Пользователю нельзя выдать Роль Супервайзера", lastToastTitle())
            assertEquals(listOf("Выдать Роль Супервайзера"), roleActions())
        }
    }

    @Nested
    inner class AccessTokenTests {
        @ParameterizedTest
        @CsvSource("1001, multiple-role-token", "1002, participant-token", "1003, observer-token")
        fun `should not show the access code of any user kind`(userId: Long, accessToken: String) {
            open(userId)

            assertEquals(emptyList<TextField>(), fields("Код-доступа"))
            assertTrue(UI.getCurrent()._find<TextField>().none { field -> field.value == accessToken })
            assertEquals(emptyList<String>(), accessTokenActions())
        }
    }

    private fun open(userId: Long) {
        UI.getCurrent().navigate(ServiceUserView::class.java, RouteParameters(SERVICE_USER_ID_PARAMETER, userId.toString()))
    }

    private fun fields(label: String): List<TextField> =
        UI.getCurrent()._find<TextField>().filter { field -> field.ariaLabel.orElse(null) == label }

    private fun textField(label: String): TextField = fields(label).single()

    private fun tags(): List<String> = block("Сведения")._find<Span> { classes = "ts-tag" }.map { tag -> tag.text }

    private fun blocks(title: String): List<Section> = UI.getCurrent()._find<Section> { classes = "ts-block" }
        .filter { block -> block._find<H3> { classes = "ts-block__title" }.single().text == title }

    private fun block(title: String): Section = blocks(title).single()

    private fun communityRows(): List<CommunityRow> = block("Сообщества")._find<TableBody>().single()._find<TableRow>()
        .filter { row -> row._find<Span> { classes = "ts-empty__title" }.isEmpty() }
        .map { row ->
            val cells = row._find<TableCell>()
            CommunityRow(
                id = cells[0].element.textRecursively,
                name = cells[1].element.textRecursively,
                roles = cells[2]._find<Span> { classes = "ts-tag" }.map { tag -> tag.text },
            )
        }

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }

    /** Texts of a row of the communities table. */
    private data class CommunityRow(val id: String, val name: String, val roles: List<String>)

    private fun roleActions(): List<String> = UI.getCurrent()._find<Button>()
        .map { button -> button.text }
        .filter { text -> text.endsWith("Роль Супервайзера") }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private fun accessTokenActions(): List<String> = UI.getCurrent()._find<Button>()
        .map { button -> button.text }
        .filter { text -> text in setOf("Показать", "Скрыть", "Копировать") }

    private companion object {
        /** Public community of the runtime settings of the service test configuration. */
        val PUBLIC_COMMUNITY_ID = CommunityId(42)

        const val OPEN_SUPERVISOR = "Открыть в Кабинете Супервайзера"
    }
}
