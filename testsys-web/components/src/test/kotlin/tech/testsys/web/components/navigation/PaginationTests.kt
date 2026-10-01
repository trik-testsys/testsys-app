package tech.testsys.web.components.navigation

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.classes
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

private const val MANY_PAGES = 20
private const val MIDDLE_PAGE = 10

class PaginationTests : MockVaadinTests() {
    @Nested
    inner class PageRangeTests {
        @Test
        fun `should show the only page of a single page`() {
            assertEquals(listOf(1), pageRange(page = 1, total = 1))
        }

        @Test
        fun `should show every page if there are at most seven`() {
            assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), pageRange(page = 4, total = 7))
        }

        @Test
        fun `should show the first five pages and the last one on the first page`() {
            assertEquals(listOf(1, 2, 3, 4, 5, null, 20), pageRange(page = 1, total = MANY_PAGES))
        }

        @Test
        fun `should show the first five pages and the last one up to the fourth page`() {
            assertEquals(listOf(1, 2, 3, 4, 5, null, 20), pageRange(page = 4, total = MANY_PAGES))
        }

        @Test
        fun `should show the neighbours of the page between gaps from the fifth page`() {
            assertEquals(listOf(1, null, 4, 5, 6, null, 20), pageRange(page = 5, total = MANY_PAGES))
        }

        @Test
        fun `should show the neighbours of a middle page between gaps`() {
            assertEquals(listOf(1, null, 9, 10, 11, null, 20), pageRange(page = MIDDLE_PAGE, total = MANY_PAGES))
        }

        @Test
        fun `should show the neighbours of the page between gaps up to the fifth page from the end`() {
            assertEquals(listOf(1, null, 15, 16, 17, null, 20), pageRange(page = 16, total = MANY_PAGES))
        }

        @Test
        fun `should show the first page and the last five from the fourth page from the end`() {
            assertEquals(listOf(1, null, 16, 17, 18, 19, 20), pageRange(page = 17, total = MANY_PAGES))
        }

        @Test
        fun `should show the first page and the last five on the last page`() {
            assertEquals(listOf(1, null, 16, 17, 18, 19, 20), pageRange(page = MANY_PAGES, total = MANY_PAGES))
        }
    }

    @Nested
    inner class MarkupTests {
        @Test
        fun `should render the pages between the previous and next buttons`() {
            buildPagination(pageCount = MANY_PAGES, page = MIDDLE_PAGE)

            val pager = ui().find("ts-pager")
            val children = pager.children.toList()
            assertEquals(testTexts.pagination.previous, children.first().element.getAttribute("aria-label"))
            assertEquals(testTexts.pagination.next, children.last().element.getAttribute("aria-label"))
            assertEquals(listOf("1", "…", "9", "10", "11", "…", "20"), children.drop(1).dropLast(1).map { child -> child.element.text })
            assertEquals(2, pager.findAll("ts-pager__gap").size)
        }

        @Test
        fun `should mark the current page only`() {
            buildPagination(pageCount = 5, page = 3)

            assertEquals(listOf("3"), activePages())
            assertEquals("page", pageButton(3).element.getAttribute("aria-current"))
            assertNull(pageButton(2).element.getAttribute("aria-current"))
            assertFalse("ts-pager__btn--active" in pageButton(2).classes())
        }

        @Test
        fun `should name the page buttons with the page texts`() {
            buildPagination(pageCount = 3)

            assertEquals(
                listOf(1, 2, 3).map { page -> testTexts.pagination.page(page) },
                numberButtons().map { button -> button.element.getAttribute("aria-label") },
            )
        }

        @Test
        fun `should render the arrows as buttons with chevrons`() {
            buildPagination(pageCount = 3)

            val previous = arrow(testTexts.pagination.previous)
            val next = arrow(testTexts.pagination.next)
            assertEquals(listOf("button", "button"), listOf(previous, next).map { button -> button.element.getAttribute("type") })
            assertTrue(previous.iconSvg().contains("""<path d="m15 18-6-6 6-6"/>"""))
            assertTrue(next.iconSvg().contains("""<path d="m9 18 6-6-6-6"/>"""))
            assertTrue(previous.iconSvg().contains("""width="14""""))
            assertTrue(next.iconSvg().contains("""width="14""""))
        }

        @Test
        fun `should disable the previous button on the first page`() {
            buildPagination(pageCount = 3, page = 1)

            assertFalse(arrow(testTexts.pagination.previous).isEnabled)
            assertTrue(arrow(testTexts.pagination.next).isEnabled)
        }

        @Test
        fun `should disable the next button on the last page`() {
            buildPagination(pageCount = 3, page = 3)

            assertTrue(arrow(testTexts.pagination.previous).isEnabled)
            assertFalse(arrow(testTexts.pagination.next).isEnabled)
        }

        @Test
        fun `should hide the pagination through the handle`() {
            val handle = buildPagination(pageCount = 3)

            handle.isVisible = false

            assertFalse(ui().find("ts-pager").isVisible)
        }
    }

    @Nested
    inner class ClickTests {
        @Test
        fun `should change the page and tell the listener on a click on a number`() {
            val handle = buildPagination(pageCount = 5, page = 1)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }

            pageButton(4)._click()

            assertEquals(4, handle.page)
            assertEquals(listOf(4), chosen)
            assertEquals(listOf("4"), activePages())
        }

        @Test
        fun `should go to the next page on a click on the next button`() {
            val handle = buildPagination(pageCount = 5, page = 2)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }

            arrow(testTexts.pagination.next)._click()

            assertEquals(3, handle.page)
            assertEquals(listOf(3), chosen)
        }

        @Test
        fun `should go to the previous page on a click on the previous button`() {
            val handle = buildPagination(pageCount = 5, page = 2)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }

            arrow(testTexts.pagination.previous)._click()

            assertEquals(1, handle.page)
            assertEquals(listOf(1), chosen)
            assertFalse(arrow(testTexts.pagination.previous).isEnabled)
        }

        @Test
        fun `should rebuild the range after a click`() {
            buildPagination(pageCount = MANY_PAGES, page = 4)

            pageButton(5)._click()

            assertEquals(listOf("1", "…", "4", "5", "6", "…", "20"), pageLabels())
        }

        @Test
        fun `should keep the arrows in place after a click`() {
            buildPagination(pageCount = 5, page = 2)
            val next = arrow(testTexts.pagination.next)
            var detachments = 0
            next.addDetachListener { detachments++ }

            next._click()

            assertEquals(0, detachments)
            assertEquals(next, ui().find("ts-pager").children.toList().last())
        }

        @Test
        fun `should focus the new current page after a click on a number`() {
            buildPagination(pageCount = 5, page = 2)
            pendingJavaScript()

            pageButton(4)._click()

            assertTrue(isFocused(pageButton(4)))
        }

        @Test
        fun `should keep the focus on an arrow that stays enabled`() {
            buildPagination(pageCount = 5, page = 2)
            pendingJavaScript()

            arrow(testTexts.pagination.next)._click()

            assertTrue(pendingJavaScript().none { call -> "focus" in call.invocation.expression })
        }

        @Test
        fun `should focus the current page when a click disables the arrow`() {
            buildPagination(pageCount = 5, page = 4)
            pendingJavaScript()

            arrow(testTexts.pagination.next)._click()

            assertTrue(isFocused(pageButton(5)))
        }

        @Test
        fun `should focus the new current page when the listener changes the page count`() {
            val handle = buildPagination(pageCount = 5, page = 2)
            handle.onChange { _ -> handle.pageCount = 8 }
            pendingJavaScript()

            pageButton(4)._click()

            assertTrue(isFocused(pageButton(4)))
        }

        @Test
        fun `should focus the new current page when the listener sets the same page count`() {
            val handle = buildPagination(pageCount = 5, page = 2)
            handle.onChange { _ -> handle.pageCount = 5 }
            pendingJavaScript()

            pageButton(4)._click()

            assertTrue(isFocused(pageButton(4)))
        }

        @Test
        fun `should focus the new current page when the bound listener sets the signal and the page count`() {
            val handle = buildPagination(pageCount = 5, page = 2)
            val signal = ValueSignal(2)
            handle.bindPage(signal)
            handle.onChange { page ->
                signal.set(page)
                handle.pageCount = 8
            }
            pendingJavaScript()

            pageButton(4)._click()

            assertTrue(isFocused(pageButton(4)))
        }

        @Test
        fun `should not tell the listener on a click on the current page`() {
            val handle = buildPagination(pageCount = 5, page = 2)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }

            pageButton(2)._click()

            assertTrue(chosen.isEmpty())
        }

        @Test
        fun `should replace the listener set by an earlier call`() {
            val handle = buildPagination(pageCount = 5)
            val first = mutableListOf<Int>()
            val second = mutableListOf<Int>()
            handle.onChange { page -> first += page }
            handle.onChange { page -> second += page }

            pageButton(3)._click()

            assertTrue(first.isEmpty())
            assertEquals(listOf(3), second)
        }
    }

    @Nested
    inner class PageTests {
        @Test
        fun `should show the page set from code without telling the listener`() {
            val handle = buildPagination(pageCount = 5)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }

            handle.page = 5

            assertTrue(chosen.isEmpty())
            assertEquals(listOf("5"), activePages())
            assertFalse(arrow(testTexts.pagination.next).isEnabled)
        }

        @Test
        fun `should reject a page below one`() {
            val handle = buildPagination(pageCount = 5)

            assertThrows<IllegalArgumentException> { handle.page = 0 }
        }

        @Test
        fun `should reject a page above the page count`() {
            val handle = buildPagination(pageCount = 5)

            assertThrows<IllegalArgumentException> { handle.page = 6 }
        }

        @Test
        fun `should keep the page after rejecting a wrong one`() {
            val handle = buildPagination(pageCount = 5, page = 2)

            assertThrows<IllegalArgumentException> { handle.page = 6 }

            assertEquals(2, handle.page)
        }
    }

    @Nested
    inner class PageCountTests {
        @Test
        fun `should clamp the page to a smaller page count`() {
            val handle = buildPagination(pageCount = 5, page = 5)

            handle.pageCount = 3

            assertEquals(3, handle.page)
            assertEquals(listOf("1", "2", "3"), pageLabels())
            assertEquals(listOf("3"), activePages())
        }

        @Test
        fun `should keep the page within a larger page count`() {
            val handle = buildPagination(pageCount = 3, page = 2)

            handle.pageCount = MANY_PAGES

            assertEquals(2, handle.page)
            assertEquals(listOf("1", "2", "3", "4", "5", "…", "20"), pageLabels())
            assertTrue(arrow(testTexts.pagination.next).isEnabled)
        }

        @Test
        fun `should not tell the listener when the page count clamps the page`() {
            val handle = buildPagination(pageCount = 5, page = 5)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }

            handle.pageCount = 3

            assertTrue(chosen.isEmpty())
        }

        @Test
        fun `should reject a page count below one`() {
            val handle = buildPagination(pageCount = 5)

            assertThrows<IllegalArgumentException> { handle.pageCount = 0 }
        }
    }

    @Nested
    inner class BindPageTests {
        @Test
        fun `should show the page of the signal`() {
            val handle = buildPagination(pageCount = 5)
            val signal = ValueSignal(2)
            handle.bindPage(signal)

            signal.set(4)

            assertEquals(4, handle.page)
            assertEquals(listOf("4"), activePages())
        }

        @Test
        fun `should tell the listener on a click while bound and keep the page of the signal`() {
            val handle = buildPagination(pageCount = 5)
            val chosen = mutableListOf<Int>()
            handle.onChange { page -> chosen += page }
            handle.bindPage(ValueSignal(2))

            pageButton(4)._click()

            assertEquals(listOf(4), chosen)
            assertEquals(2, handle.page)
            assertEquals(listOf("2"), activePages())
        }

        @Test
        fun `should show the page chosen by a click once the signal takes it`() {
            val handle = buildPagination(pageCount = 5)
            val signal = ValueSignal(2)
            handle.bindPage(signal)
            handle.onChange { page -> signal.set(page) }

            pageButton(4)._click()

            assertEquals(4, handle.page)
            assertEquals(listOf("4"), activePages())
        }

        @Test
        fun `should show the page of the signal again when the page count grows back while bound`() {
            val handle = buildPagination(pageCount = 5)
            handle.bindPage(ValueSignal(5))
            handle.pageCount = 3

            handle.pageCount = 5

            assertEquals(5, handle.page)
            assertEquals(listOf("5"), activePages())
        }

        @Test
        fun `should show the clamped page of the signal while the page count is smaller`() {
            val handle = buildPagination(pageCount = 5)
            handle.bindPage(ValueSignal(5))

            handle.pageCount = 3

            assertEquals(3, handle.page)
            assertEquals(listOf("3"), activePages())
        }

        @Test
        fun `should go to the clicked page through the signal while the page count clamps the page of the signal`() {
            val handle = buildPagination(pageCount = 5)
            val signal = ValueSignal(5)
            handle.bindPage(signal)
            handle.onChange { page -> signal.set(page) }
            handle.pageCount = 3

            pageButton(2)._click()

            assertEquals(2, handle.page)
            assertEquals(2, signal.peek())
            assertEquals(listOf("2"), activePages())
        }

        @Test
        fun `should reject a manual page while bound`() {
            val handle = buildPagination(pageCount = 5)
            handle.bindPage(ValueSignal(2))

            assertThrows<BindingActiveException> { handle.page = 3 }
        }

        @Test
        fun `should reject a second binding`() {
            val handle = buildPagination(pageCount = 5)
            handle.bindPage(ValueSignal(2))

            assertThrows<BindingActiveException> { handle.bindPage(ValueSignal(3)) }
        }
    }

    @Nested
    inner class CreationTests {
        @Test
        fun `should start on the first page by default`() {
            val handle = buildPagination(pageCount = 3)

            assertEquals(1, handle.page)
            assertEquals(3, handle.pageCount)
        }

        @Test
        fun `should apply the configuration to the handle`() {
            val chosen = mutableListOf<Int>()
            buildTestContent { pagination(pageCount = 3) { onChange { page -> chosen += page } } }

            pageButton(2)._click()

            assertEquals(listOf(2), chosen)
        }

        @Test
        fun `should reject a page count below one`() {
            assertThrows<IllegalArgumentException> { buildPagination(pageCount = 0) }
        }

        @Test
        fun `should reject an initial page below one`() {
            assertThrows<IllegalArgumentException> { buildPagination(pageCount = 3, page = 0) }
        }

        @Test
        fun `should reject an initial page above the page count`() {
            assertThrows<IllegalArgumentException> { buildPagination(pageCount = 3, page = 4) }
        }
    }

    private fun buildPagination(pageCount: Int, page: Int = 1): PaginationHandle {
        lateinit var handle: PaginationHandle
        buildTestContent { handle = pagination(pageCount = pageCount, page = page) }
        return handle
    }

    private fun ui(): Component = UI.getCurrent()

    private fun buttons(): List<NativeButton> = ui().findAll("ts-pager__btn").map { button -> button as NativeButton }

    private fun numberButtons(): List<NativeButton> = buttons().filter { button -> button.element.text.isNotEmpty() }

    private fun activeButtons(): List<NativeButton> = buttons().filter { button -> "ts-pager__btn--active" in button.classes() }

    private fun pageButton(page: Int): NativeButton = numberButtons().single { button -> button.element.text == page.toString() }

    private fun arrow(label: String): NativeButton = buttons().single { button -> button.element.getAttribute("aria-label") == label }

    private fun NativeButton.iconSvg(): String = find("ts-icon").element.getProperty("innerHTML")

    private fun activePages(): List<String> = activeButtons().map { button -> button.element.text }

    private fun pendingJavaScript(): List<PendingJavaScriptInvocation> {
        val internals = UI.getCurrent().internals
        internals.stateTree.runExecutionsBeforeClientResponse()
        return internals.dumpPendingJavaScriptInvocations()
    }

    private fun isFocused(button: NativeButton): Boolean =
        pendingJavaScript().any { call -> call.owner == button.element.node && "focus" in call.invocation.expression }

    private fun pageLabels(): List<String> = ui().find("ts-pager").children.toList().drop(1).dropLast(1).map { child -> child.element.text }
}
