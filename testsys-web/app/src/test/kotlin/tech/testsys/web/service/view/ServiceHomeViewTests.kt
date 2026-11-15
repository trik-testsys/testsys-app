package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableCell
import com.vaadin.flow.component.html.TableRow
import io.mockk.clearMocks
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.infra.database.api.session.CabinetSignInStore
import tech.testsys.web.app.view.clickRow
import tech.testsys.web.service.SERVICE_TEST_GRAFANA_URL
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.SERVICE_TEST_VERSION
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.security.ServiceSignIn

class ServiceHomeViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var signIn: ServiceSignIn

    @Autowired
    private lateinit var store: CabinetSignInStore

    @BeforeEach
    fun openHome() {
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceHomeView::class.java)
        pendingJavaScript()
    }

    @Test
    fun `should show the version of the system in the footer`() {
        assertEquals(SERVICE_TEST_VERSION, UI.getCurrent()._get<Span> { classes = "ts-footer__version" }.text)
    }

    @Test
    fun `should list the sections of every menu group with their purpose`() {
        val groups = UI.getCurrent()._find<Section> { classes = "ts-block" }.map { block ->
            block._get<H3> { classes = "ts-block__title" }.text to rowsOf(block).map(::sectionOf)
        }

        assertEquals(
            listOf(
                "Система" to listOf("Настройки", "Проверка", "Версии TRIK Studio"),
                "Данные" to listOf("Пользователи", "Объявления"),
                "Инструменты" to listOf("Дешифровка ID", "Grafana"),
            ),
            groups,
        )
        assertTrue(sectionRow("Пользователи").element.textRecursively.contains("Поиск Пользователей и их карточки"))
    }

    @ParameterizedTest
    @CsvSource(
        "Настройки, settings",
        "Проверка, grading",
        "Версии TRIK Studio, trik-studio-versions",
        "Пользователи, users",
        "Объявления, announcements",
        "Дешифровка ID, id-decoding",
    )
    fun `should open the section of a clicked row`(section: String, path: String) {
        clickRow(sectionRow(section))

        assertEquals(path, UI.getCurrent().internals.activeViewLocation.path)
    }

    @Test
    fun `should open only the configured Grafana address from its row in the same tab without passing service credentials`() {
        clickRow(sectionRow("Grafana"))

        val transitions = pendingJavaScript().filter { call -> "window.open" in call.invocation.expression }
        assertEquals(listOf(listOf(SERVICE_TEST_GRAFANA_URL, "_self")), transitions.map { call -> call.invocation.parameters })
        assertEquals(ServiceHomeView::class.java, currentView)
    }

    @Test
    fun `should open only the configured Grafana address from the header menu in the same tab`() {
        val grafana = UI.getCurrent()._find<NativeButton> { classes = "ts-mega__link" }
            .single { link -> link.element.textRecursively == "Grafana" }

        grafana._click()

        val transitions = pendingJavaScript().filter { call -> "window.open" in call.invocation.expression }
        assertEquals(listOf(listOf(SERVICE_TEST_GRAFANA_URL, "_self")), transitions.map { call -> call.invocation.parameters })
    }

    @Test
    fun `should sign the service out from the header menu`() {
        UI.getCurrent()._get<NativeButton> { text = "Выйти" }._click()

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `should forget the remembered service sign-in on signing out`() {
        clearMocks(store)

        UI.getCurrent()._get<NativeButton> { text = "Выйти" }._click()

        verify { store.delete(any()) }
    }

    private fun rowsOf(block: Section): List<TableRow> = block._get<TableBody>()._find<TableRow>()

    private fun sectionRow(label: String): TableRow = UI.getCurrent()._find<TableBody>().flatMap { body -> body._find<TableRow>() }
        .single { row -> sectionOf(row) == label }

    private fun sectionOf(row: TableRow): String = row._find<TableCell>().first().element.textRecursively

    private fun pendingJavaScript() = UI.getCurrent().internals.let { internals ->
        internals.stateTree.runExecutionsBeforeClientResponse()
        internals.dumpPendingJavaScriptInvocations()
    }
}
