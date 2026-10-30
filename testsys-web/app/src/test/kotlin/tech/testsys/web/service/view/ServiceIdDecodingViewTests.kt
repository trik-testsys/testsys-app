package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.security.ServiceSignIn
import java.time.Instant

class ServiceIdDecodingViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var signIn: ServiceSignIn

    @BeforeEach
    fun openPage() {
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceIdDecodingView::class.java)
    }

    @Test
    fun `should open the decoding page from the service menu`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        clickMenuLink("Дешифровка ID")

        assertEquals(ServiceIdDecodingView::class.java, currentView)
    }

    @Test
    fun `should show the moment in UTC, the node id and the counter of the entered id`() {
        val id = (Instant.parse("2026-10-30T12:34:56Z").epochSecond shl 26) or (7L shl 16) or 1L

        textField("ID")._value = " $id "
        decode()

        assertFalse(textField("ID").isInvalid)
        assertEquals("2026-10-30T12:34:56Z", textField("Создан (UTC)").value)
        assertEquals("7", textField("Узел").value)
        assertEquals("1", textField("Счётчик").value)
    }

    @ParameterizedTest
    @CsvSource("12a, Введите число", "-1, Система не выдаёт такой ID")
    fun `should reject an id that is not issued by the system and clear the previous result`(id: String, error: String) {
        textField("ID")._value = "1"
        decode()

        textField("ID")._value = id
        decode()

        assertTrue(textField("ID").isInvalid)
        assertEquals(error, textField("ID").errorMessage)
        assertEquals("", textField("Создан (UTC)").value)
    }

    private fun decode() {
        UI.getCurrent()._get<Button> { text = "Расшифровать" }._click()
    }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }
}
