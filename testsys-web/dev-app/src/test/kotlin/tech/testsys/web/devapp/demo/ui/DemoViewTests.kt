package tech.testsys.web.devapp.demo.ui

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import tech.testsys.web.devapp.MockSpringVaadinTests
import tech.testsys.web.devapp.demo.DemoSession
import tech.testsys.web.devapp.demo.model.createCompetition
import tech.testsys.web.devapp.demo.model.login

@SpringBootTest
@ActiveProfiles("dev")
class DemoViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var context: ApplicationContext

    private val session: DemoSession
        get() = context.getBean(DemoSession::class.java)

    @BeforeEach
    fun resetDemo() {
        session.reset()
    }

    @Test
    fun `should show one success toast and never replay it as page content`() {
        UI.getCurrent().navigate(DemoView::class.java, parameters("organizer.competitions"))
        UI.getCurrent()._get<Button> { text = "Создать соревнование" }._click()
        val dialog = UI.getCurrent()._find<Dialog>().single { it.isOpened }
        dialog._get<TextField>().value = "Новое"
        dialog._get<Button> { text = "Создать" }._click()
        assertEquals(1, UI.getCurrent()._find<Notification>().size)
        assertTrue(UI.getCurrent()._find<Div> { classes = "ts-toast--success" }.isNotEmpty())
        assertFalse(UI.getCurrent()._find<com.vaadin.flow.component.html.H3>().any { it.text == "Сообщение" })
        UI.getCurrent().navigate(DemoView::class.java, parameters("student.profile"))
        assertEquals(1, UI.getCurrent()._find<Notification>().size)
    }

    @Test
    fun `should show one error toast without changing records or storing message content`() {
        UI.getCurrent().navigate(DemoView::class.java, parameters("login"))
        val context = DemoContext(session, "login", {}, {})
        assertFalse(context.apply(session.state.login("missing")))
        assertEquals(1, UI.getCurrent()._find<Notification>().size)
        assertTrue(UI.getCurrent()._find<Div> { classes = "ts-toast--error" }.isNotEmpty())
        assertEquals(null, session.state.sessionUserId)
        UI.getCurrent().navigate(DemoView::class.java, parameters("login"))
        assertEquals(1, UI.getCurrent()._find<Notification>().size)
        assertFalse(UI.getCurrent()._find<com.vaadin.flow.component.html.H3>().any { it.text == "Сообщение" })
    }

    @Test
    fun `should retain all profile information in ordered pairs`() {
        UI.getCurrent().navigate(DemoView::class.java, parameters("student.profile"))
        val rows = UI.getCurrent()._find<Div> { classes = "ts-block__row" }
        assertEquals(3, rows.size)
        assertEquals(listOf(2, 2, 2), rows.map { row -> row.children.filter { it.element.classList.contains("ts-field") }.count().toInt() })
        assertEquals(listOf("ID", "Псевдоним", "Роль", "Код-доступа", "Почта", "Последний вход"),
            UI.getCurrent()._find<com.vaadin.flow.component.html.Span> { classes = "ts-field__text" }.map { it.text })
        assertEquals(6, UI.getCurrent()._find<TextField>().size)
    }

    @Test
    fun `should validate competition dialog cancel without changes and reopen with defaults`() {
        UI.getCurrent().navigate(DemoView::class.java, parameters("organizer.competitions"))
        UI.getCurrent()._get<Button> { text = "Создать соревнование" }._click()
        val dialog = UI.getCurrent()._find<Dialog>().single { it.isOpened }

        dialog._get<Button> { text = "Создать" }._click()
        assertTrue(dialog.isOpened)
        assertEquals(1, session.state.competitions.size)
        val name = dialog._get<TextField>()
        assertTrue(name.isInvalid)
        name.value = "Черновик"
        dialog._get<Button> { text = "Отмена" }._click()
        assertFalse(dialog.isOpened)
        assertEquals(1, session.state.competitions.size)
        UI.getCurrent()._get<Button> { text = "Создать соревнование" }._click()
        assertEquals("", name.value)
        name.value = "Новое соревнование"
        dialog._get<Button> { text = "Создать" }._click()
        assertEquals("Новое соревнование", session.state.competitions.last().name)
        assertEquals("competition103", session.selections["organizer:competition"])
    }

    @Test
    fun `should default participant quantity to three and preserve records on invalid input`() {
        UI.getCurrent().navigate(DemoView::class.java, parameters("organizer.participants"))
        UI.getCurrent()._get<Button> { text = "Создать участников" }._click()
        val dialog = UI.getCurrent()._find<Dialog>().single { it.isOpened }
        val count = dialog._get<IntegerField>()
        assertEquals(3, count.value)
        count.value = 0
        dialog._get<Button> { text = "Создать" }._click()
        assertTrue(dialog.isOpened)
        assertEquals(9, session.state.users.size)
        count.value = 12
        dialog._get<Button> { text = "Создать" }._click()
        assertEquals(21, session.state.users.size)
        assertEquals(14, session.state.competitions.first().participantIds.size)
    }

    @Test
    fun `should retain demo state between role navigation and keep one footer`() {
        UI.getCurrent().navigate(DemoView::class.java, parameters("organizer.competitions"))
        session.state = session.state.createCompetition("Сохраняется").state
        UI.getCurrent().navigate(DemoView::class.java, parameters("student.profile"))
        UI.getCurrent().navigate(DemoView::class.java, parameters("organizer.competitions"))

        assertEquals(2, session.state.competitions.size)
        val footerCount = UI.getCurrent()._find<com.vaadin.flow.component.html.Footer>().size
        assertEquals(1, footerCount)
        assertTrue(UI.getCurrent()._find<NativeButton>().any { it.text == "Фильтры" || it.element.getAttribute("aria-expanded") == "false" })
    }
}
