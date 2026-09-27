package tech.testsys.web.ui

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.navigation.CabinetHeader

/** Route target with a parameter whose head title comes from `beforeEnter`, as a page with route parameters does. */
@Route("test/param/:id")
class ParamTestView : TestSysView(testTexts), BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val id = event.routeParameters.get("id").orElseThrow()
        page(CabinetHeader()) { head(id) }
    }
}

/** Route target whose page tabs include itself, to check that [TestSysView.page] marks its own tab as current. */
@Route("test/self")
class SelfTabTestView : TestSysView(testTexts) {
    init {
        page(CabinetHeader()) {
            head("Тур") {
                tabs {
                    tab("Эта", SelfTabTestView::class.java)
                    tab("Другая", FirstTestView::class.java)
                }
            }
        }
    }
}

class TestSysViewTests : MockVaadinTests() {
    private class SampleView(body: PageScope.() -> Unit) : TestSysView(testTexts) {
        init {
            page(CabinetHeader(), body)
        }
    }

    private class NestedPageView : TestSysView(testTexts) {
        init {
            page(CabinetHeader()) { page(CabinetHeader()) {} }
        }
    }

    private class RepeatedPageView : TestSysView(testTexts) {
        init {
            page(CabinetHeader()) { block { row { text("first") } } }
            page(CabinetHeader()) { block { row { text("second") } } }
        }
    }

    @Test
    fun `should build app root with header and page body`() {
        val root = SampleView { block { row { text("x") } } }.child(0)

        assertTrue("ts-app" in root.classes())
        assertTrue("ts-header" in root.child(0).classes())
        assertEquals("main", root.child(1).element.tag)
        assertTrue("ts-page" in root.child(1).classes())
    }

    @Test
    fun `should reject a page call inside the page body`() {
        val error = assertThrows<IllegalStateException> { NestedPageView() }

        assertTrue("NestedPageView" in error.message.orEmpty())
    }

    @Test
    fun `should rebuild the page on a repeated call`() {
        val root = RepeatedPageView().child(0)

        val rows = root.findAll("ts-block__row")
        assertEquals(1, rows.size)
        assertEquals("second", rows.single().element.textRecursively)
    }

    @Test
    fun `should rebuild the page when navigating again with different route parameters`() {
        UI.getCurrent().navigate(ParamTestView::class.java, RouteParameters("id", "1"))

        UI.getCurrent().navigate(ParamTestView::class.java, RouteParameters("id", "2"))

        assertEquals("2", UI.getCurrent().find("ts-h1").element.text)
    }

    @Test
    fun `should mark its own tab as current when navigating to the view`() {
        UI.getCurrent().navigate(SelfTabTestView::class.java)

        assertTrue("ts-tab--active" in UI.getCurrent().findAll("ts-tab")[0].classes())
    }
}
