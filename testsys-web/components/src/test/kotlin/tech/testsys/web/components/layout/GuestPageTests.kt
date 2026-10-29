package tech.testsys.web.components.layout

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.button
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.segmentedControl
import tech.testsys.web.components.forms.textInput
import java.time.Year

class GuestPageTests : MockVaadinTests() {
    private class GuestView(body: GuestPageScope.() -> Unit) : TestSysView() {
        init {
            guestPage("Tagline", body)
        }
    }

    private class NestedGuestView : TestSysView() {
        init {
            guestPage("Tagline") { guestPage("Tagline") {} }
        }
    }

    private class RepeatedGuestView : TestSysView() {
        init {
            guestPage("Tagline") { card("First", "Description") {} }
            guestPage("Tagline") { card("Second", "Description") {} }
        }
    }

    private fun build(body: GuestPageScope.() -> Unit) = GuestView(body).also { view -> UI.getCurrent().add(view) }.child(0)

    @Test
    fun `should build the guest root without a header, a page head and guides`() {
        val root = build { card("Вход", "Описание") {} }

        assertTrue("ts-app--guest" in root.classes())
        assertEquals(listOf("main", "footer"), root.children.map { child -> child.element.tag }.toList())
        assertTrue(root.findAll("ts-header").isEmpty())
        assertTrue(root.findAll("ts-page-head").isEmpty())
        assertTrue(root.findAll("ts-guide-button").isEmpty())
        assertTrue(root.findAll("ts-guide-controller").isEmpty())
    }

    @Test
    fun `should show the named brand and the tagline above the card`() {
        val main = build { card("Вход", "Описание") {} }.child(0)

        assertEquals("TestSys", main.child(0).element.getAttribute("aria-label"))
        assertTrue("ts-guest__brand" in main.child(0).classes())
        assertEquals("Tagline", main.child(1).element.text)
        assertTrue("ts-guest-card" in main.child(2).classes())
    }

    @Test
    fun `should show the title, the description and the step of the card`() {
        val card = build { card("Регистрация", "Укажите почту.", step = "Шаг 1 из 2") {} }.find("ts-guest-card")

        assertEquals("Регистрация", card.find("ts-guest-card__title").element.text)
        assertEquals("h1", card.find("ts-guest-card__title").element.tag)
        assertEquals("Шаг 1 из 2", card.find("ts-guest-card__step").element.text)
        assertEquals("Шаг 1 из 2Укажите почту.", card.find("ts-guest-card__desc").element.textRecursively)
    }

    @Test
    fun `should show no step label without a step`() {
        val card = build { card("Вход", "Описание") {} }.find("ts-guest-card")

        assertTrue(card.findAll("ts-guest-card__step").isEmpty())
    }

    @Test
    fun `should reject a second card`() {
        assertThrows<IllegalStateException> {
            build {
                card("Вход", "Описание") {}
                card("Вход", "Описание") {}
            }
        }
    }

    @Test
    fun `should reject a blank card title`() {
        assertThrows<IllegalArgumentException> { build { card(" ", "Описание") {} } }
    }

    @Test
    fun `should reject a blank card description`() {
        assertThrows<IllegalArgumentException> { build { card("Вход", "") {} } }
    }

    @Test
    fun `should place the label above the control and name the control by it`() {
        val root = build { card("Вход", "Описание") { textInput("Почта", hint = "Подсказка") } }

        val field = root.find("ts-field--stacked")
        assertInstanceOf(NativeLabel::class.java, field.child(0).child(0))
        assertEquals("Почта", field.child(0).find("ts-field__text").element.text)
        val control = field.find("ts-field__value").child(0) as TextField
        assertEquals("Почта", control.ariaLabel.orElseThrow())
        assertEquals("Подсказка", control.helperText)
    }

    @Test
    fun `should put the aside link in the label row`() {
        val root = build {
            card("Вход", "Описание") { passwordInput("Пароль", aside = { linkAction("Забыли пароль?") }) }
        }

        val aside = root.find("ts-field__label-row").find("ts-field__aside")
        assertEquals("Забыли пароль?", aside.child(0).element.text)
    }

    @Test
    fun `should hide the label together with a hidden field`() {
        val root = build { card("Вход", "Описание") { passwordInput("Код") { isVisible = false } } }

        assertFalse(root.find("ts-field--stacked").isVisible)
        assertTrue(root.find("ts-field--stacked").element.textRecursively.contains("Код"))
    }

    @Test
    fun `should add the segmented choice and the main action to the card body`() {
        val root = build {
            card("Вход", "Описание") {
                segmentedControl("Способ", items = listOf("A", "B"), itemLabel = { item -> item }) { value = "A" }
                mainAction("Войти")
            }
        }

        val body = root.find("ts-guest-card__body")
        assertInstanceOf(RadioButtonGroup::class.java, body.child(0).child(0))
        assertEquals("main", button("Войти").element.getAttribute("data-ts-role"))
        assertEquals(body, button("Войти").parent.orElseThrow())
    }

    @Test
    fun `should put the links under the card and separate consecutive link actions`() {
        val main = build {
            links {
                linkAction("Изменить почту")
                linkAction("Вернуться ко входу")
            }
            card("Вход", "Описание") {}
        }.child(0)

        assertTrue("ts-guest-card" in main.child(2).classes())
        val links = main.child(3)
        assertTrue("ts-guest__links" in links.classes())
        assertEquals(listOf("Изменить почту", "·", "Вернуться ко входу"), links.children.map { link -> link.element.text }.toList())
        assertEquals("true", links.child(1).element.getAttribute("aria-hidden"))
    }

    @Test
    fun `should not separate a text from a link action`() {
        val links = build {
            links {
                text("Нет аккаунта?")
                linkAction("Зарегистрироваться")
            }
        }.find("ts-guest__links")

        assertEquals(2, links.children.count())
    }

    @Test
    fun `should reject second links`() {
        assertThrows<IllegalStateException> {
            build {
                links {}
                links {}
            }
        }
    }

    @Test
    fun `should show the footer with the year`() {
        val footer = build {}.child(1)

        assertTrue(footer.element.textRecursively.contains(Year.now().value.toString()))
    }

    @Test
    fun `should reject a guest page call inside the body`() {
        val error = assertThrows<IllegalStateException> { NestedGuestView() }

        assertTrue("NestedGuestView" in error.message.orEmpty())
    }

    @Test
    fun `should replace the body on a repeated call`() {
        val root = RepeatedGuestView().child(0)

        assertEquals(listOf("Second"), root.findAll("ts-guest-card__title").map { title -> title.element.text })
        assertEquals(1, root.findAll("ts-footer").size)
    }
}
