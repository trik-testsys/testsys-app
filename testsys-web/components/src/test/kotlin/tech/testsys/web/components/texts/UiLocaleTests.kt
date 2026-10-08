package tech.testsys.web.components.texts

import com.vaadin.flow.component.UI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.pendingJavaScript
import tech.testsys.web.components.testTexts
import java.util.Locale

class UiLocaleTests : MockVaadinTests() {
    @Test
    fun `should set the locale of the UI from the texts`() {
        initializeUiLocale(UI.getCurrent(), testTexts)

        assertEquals(Locale.forLanguageTag("ru-RU"), UI.getCurrent().locale)
    }

    @Test
    fun `should set the document language to the language tag of the texts`() {
        pendingJavaScript()

        initializeUiLocale(UI.getCurrent(), testTexts)

        assertTrue(
            pendingJavaScript().any { call ->
                "document.documentElement.lang = \$0" in call.invocation.expression &&
                    call.invocation.parameters.any { parameter -> "ru-RU" in parameter.toString() }
            },
        )
    }
}
