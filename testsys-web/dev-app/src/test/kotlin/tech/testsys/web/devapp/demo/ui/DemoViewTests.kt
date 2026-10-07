package tech.testsys.web.devapp.demo.ui

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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

    @Nested
    inner class FeedbackTests {
        @Test
        fun `should show one success toast without a message block after creating a competition`() {
            val dialog = openDialog(route = "organizer.competitions", action = "Создать соревнование")
            dialog._get<TextField>().value = "Новое"

            dialog._get<Button> { text = "Создать" }._click()

            assertEquals(1, UI.getCurrent()._find<Notification>().size)
            assertTrue(UI.getCurrent()._find<Div> { classes = "ts-toast--success" }.isNotEmpty())
            assertFalse(UI.getCurrent()._find<H3>().any { heading -> heading.text == "Сообщение" })
        }

        @Test
        fun `should not replay a success toast after navigation`() {
            val dialog = openDialog(route = "organizer.competitions", action = "Создать соревнование")
            dialog._get<TextField>().value = "Новое"
            dialog._get<Button> { text = "Создать" }._click()

            navigate("student.profile")

            assertEquals(1, UI.getCurrent()._find<Notification>().size)
        }

        @Test
        fun `should show one error toast without changing records for a wrong access code`() {
            navigate("login")

            val isApplied = DemoContext(session, "login", {}, {}).apply(session.state.login("missing"))

            assertFalse(isApplied)
            assertEquals(1, UI.getCurrent()._find<Notification>().size)
            assertTrue(UI.getCurrent()._find<Div> { classes = "ts-toast--error" }.isNotEmpty())
            assertNull(session.state.sessionUserId)
        }

        @Test
        fun `should not replay an error toast as page content after navigation`() {
            navigate("login")
            DemoContext(session, "login", {}, {}).apply(session.state.login("missing"))

            navigate("login")

            assertEquals(1, UI.getCurrent()._find<Notification>().size)
            assertFalse(UI.getCurrent()._find<H3>().any { heading -> heading.text == "Сообщение" })
        }
    }

    @Test
    fun `should retain all profile information in ordered pairs`() {
        navigate("student.profile")

        val rows = UI.getCurrent()._find<Div> { classes = "ts-block__row" }
        val fieldsPerRow = rows.map { row -> row.children.filter { child -> child.element.classList.contains("ts-field") }.count().toInt() }
        assertEquals(listOf(2, 2, 2), fieldsPerRow)
        assertEquals(
            listOf("ID", "Псевдоним", "Роль", "Код-доступа", "Почта", "Последний вход"),
            UI.getCurrent()._find<Span> { classes = "ts-field__text" }.map { caption -> caption.text },
        )
        assertEquals(6, UI.getCurrent()._find<TextField>().size)
    }

    @Nested
    inner class CompetitionDialogTests {
        @Test
        fun `should keep the dialog open with an error when the name is empty`() {
            val dialog = openDialog(route = "organizer.competitions", action = "Создать соревнование")

            dialog._get<Button> { text = "Создать" }._click()

            assertTrue(dialog.isOpened)
            assertTrue(dialog._get<TextField>().isInvalid)
            assertEquals(1, session.state.competitions.size)
        }

        @Test
        fun `should close the dialog without changes on cancel`() {
            val dialog = openDialog(route = "organizer.competitions", action = "Создать соревнование")
            dialog._get<TextField>().value = "Черновик"

            dialog._get<Button> { text = "Отменить" }._click()

            assertFalse(dialog.isOpened)
            assertEquals(1, session.state.competitions.size)
        }

        @Test
        fun `should reopen the dialog with defaults`() {
            val dialog = openDialog(route = "organizer.competitions", action = "Создать соревнование")
            dialog._get<TextField>().value = "Черновик"
            dialog._get<Button> { text = "Отменить" }._click()

            UI.getCurrent()._get<Button> { text = "Создать соревнование" }._click()

            assertEquals("", dialog._get<TextField>().value)
        }

        @Test
        fun `should create and select a competition`() {
            val dialog = openDialog(route = "organizer.competitions", action = "Создать соревнование")
            dialog._get<TextField>().value = "Новое соревнование"

            dialog._get<Button> { text = "Создать" }._click()

            assertEquals("Новое соревнование", session.state.competitions.last().name)
            assertEquals("competition103", session.selections["organizer:competition"])
        }
    }

    @Nested
    inner class ParticipantsDialogTests {
        @Test
        fun `should default participant quantity to three`() {
            val dialog = openDialog(route = "organizer.participants", action = "Создать участников")

            assertEquals(3, dialog._get<IntegerField>().value)
        }

        @Test
        fun `should keep records and the dialog on an invalid quantity`() {
            val dialog = openDialog(route = "organizer.participants", action = "Создать участников")
            dialog._get<IntegerField>().value = 0

            dialog._get<Button> { text = "Создать" }._click()

            assertTrue(dialog.isOpened)
            assertEquals(9, session.state.users.size)
        }

        @Test
        fun `should create the requested quantity of participants`() {
            val dialog = openDialog(route = "organizer.participants", action = "Создать участников")
            dialog._get<IntegerField>().value = 12

            dialog._get<Button> { text = "Создать" }._click()

            assertEquals(21, session.state.users.size)
            assertEquals(14, session.state.competitions.first().participantIds.size)
        }
    }

    @Nested
    inner class NavigationTests {
        @Test
        fun `should retain demo state between role navigation`() {
            navigate("organizer.competitions")
            session.state = session.state.createCompetition("Сохраняется").state
            navigate("student.profile")

            navigate("organizer.competitions")

            assertEquals(2, session.state.competitions.size)
        }

        @Test
        fun `should keep one footer after role navigation`() {
            navigate("organizer.competitions")
            navigate("student.profile")

            navigate("organizer.competitions")

            assertEquals(1, UI.getCurrent()._find<Footer>().size)
        }

        @Test
        fun `should show the collapsed table filters after role navigation`() {
            navigate("organizer.competitions")
            navigate("student.profile")

            navigate("organizer.competitions")

            val toggle = UI.getCurrent()._find<NativeButton> { classes = "ts-table-filters__toggle" }.first()
            assertEquals("Настроить фильтры", toggle.text)
            assertEquals("false", toggle.element.getAttribute("aria-expanded"))
        }
    }

    private fun navigate(screen: String) {
        UI.getCurrent().navigate(DemoView::class.java, parameters(screen))
    }

    private fun openDialog(route: String, action: String): Dialog {
        navigate(route)
        UI.getCurrent()._get<Button> { text = action }._click()
        return UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }
    }
}
