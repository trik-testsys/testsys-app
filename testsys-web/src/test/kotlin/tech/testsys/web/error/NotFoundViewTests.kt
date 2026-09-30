package tech.testsys.web.error

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.expectView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.NotFoundException
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.MockSpringVaadinTests
import tools.jackson.databind.node.BooleanNode

@SpringBootTest
class NotFoundViewTests : MockSpringVaadinTests() {
    @Test
    fun `should route an unknown address to branded localized error without details`() {
        val ui = UI.getCurrent()
        val status = ui.internals.router.navigate(ui, Location("missing-sensitive-route"), NavigationTrigger.PAGE_LOAD)

        assertEquals(404, status)

        expectView<NotFoundView>()
        assertEquals("Страница не найдена", _get<H1>().text)
        assertEquals("TestSys", _get<Span> { classes = "ts-brand" }.text)
        assertEquals("Страница не найдена — TestSys", _get<NotFoundView>().pageTitle)
        val content = UI.getCurrent().element.textRecursively
        assertFalse(content.contains("missing-sensitive-route"))
        assertFalse(content.contains("dev/showcase"))
        assertFalse(content.contains("Exception"))
        assertEquals(listOf("Назад"), _find<Button>().map { button -> button.text })
        assertFalse(_get<Button>().isEnabled)
    }

    @Test
    fun `should return 404 and reset back availability whenever displayed`() {
        val ui = UI.getCurrent()
        ui.internals.router.navigate(ui, Location("missing"), NavigationTrigger.PAGE_LOAD)
        val view = _get<NotFoundView>()
        _get<Button>().isEnabled = true

        val status = view.setErrorParameter(mockk(), ErrorParameter(NotFoundException::class.java, NotFoundException("private details")))

        assertEquals(404, status)
        assertFalse(_get<Button>().isEnabled)
    }
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should keep back disabled until client reports completed history`(canGoBack: Boolean) {
        val ui = UI.getCurrent()
        ui.internals.router.navigate(ui, Location("missing"), NavigationTrigger.PAGE_LOAD)
        ui.internals.stateTree.runExecutionsBeforeClientResponse()
        val history = ui.internals.dumpPendingJavaScriptInvocations()
            .single { call -> "canGoBack" in call.invocation.expression }
        val back = _get<Button>()
        assertFalse(back.isEnabled)

        history.complete(BooleanNode.valueOf(canGoBack))

        assertEquals(canGoBack, back.isEnabled)
    }
}
