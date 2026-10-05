package tech.testsys.web.components

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.display.text
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.header.CabinetHeader

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
            page(CabinetHeader()) {
                footer { link(label = "First", href = "/first") }
                block { row { text("first") } }
            }
            page(CabinetHeader()) {
                footer { link(label = "Second", href = "/second") }
                block { row { text("second") } }
            }
        }
    }

    @Test
    fun `should build app root with header and page body`() {
        val root = SampleView { block { row { text("x") } } }.child(0)

        assertTrue("ts-app" in root.classes())
        assertTrue("ts-header" in root.child(0).classes())
        assertEquals("main", root.child(1).element.tag)
        assertTrue("ts-page" in root.child(1).classes())
        assertEquals("footer", root.child(2).element.tag)
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
        assertEquals(1, root.findAll("ts-footer").size)
        assertEquals(listOf("/second"), root.findAll("ts-footer__link").map { link -> link.element.getAttribute("href") })
    }

    @Test
    internal fun `should place the footer after main when the page also has a head`() {
        val root = SampleView {
            head("Sample")
            footer { link("Other view", SecondTestView::class.java) }
            block {}
        }.child(0)

        assertEquals("main", root.child(2).element.tag)
        assertEquals("footer", root.child(3).element.tag)
        assertEquals("test/second", root.find("ts-footer__link").element.getAttribute("href"))
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
