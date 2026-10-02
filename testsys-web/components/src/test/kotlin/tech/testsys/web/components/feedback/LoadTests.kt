package tech.testsys.web.components.feedback

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.Background
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.button
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.control
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.text
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockHandle
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.navigation.tabs
import tech.testsys.web.components.testTexts
import java.util.concurrent.Executor
import kotlin.concurrent.thread

class LoadTests : MockVaadinTests() {
    private val queue = QueueExecutor()

    @BeforeEach
    fun useQueue() {
        Background.executorOverride = queue
    }

    @AfterEach
    fun resetExecutor() {
        Background.executorOverride = null
    }

    @Test
    fun `should request focus in replaced body only when user starts editing`() {
        lateinit var handle: LoadHandle
        var revision = 0
        buildTestPage {
            block {
                editing(onSave = { true }, onCancel = {})
                handle = load({ ++revision }) { value -> row { textInput("Поле $value", labelSize = 4, size = 20) } }
            }
        }
        queue.runAll()
        val old = control<TextField>("Поле 1")
        pendingJavaScript()
        handle.reload()
        queue.runAll()
        val current = control<TextField>("Поле 2")
        pendingJavaScript()

        button("Изменить")._click()

        assertFalse(old.isAttached)
        assertTrue(current.isAttached)
        assertTrue(pendingJavaScript().any { call -> call.owner == button("Отменить").element.node && "focus" in call.invocation.expression })
    }

    private fun pendingJavaScript(): List<PendingJavaScriptInvocation> {
        val internals = UI.getCurrent().internals
        internals.stateTree.runExecutionsBeforeClientResponse()
        return internals.dumpPendingJavaScriptInvocations()
    }

    @Nested
    inner class SkeletonTests {
        @Test
        fun `should show skeleton rows while loading`() {
            buildTestPage { block(title = "Туры") { load({ 1 }, skeletonRows = 4) { value -> row { text("Всего: $value") } } } }

            assertEquals(4, ui().findAll("ts-skel-row").size)
            assertTrue(ui().findAll("ts-block__row").isEmpty())
        }

        @Test
        fun `should show three skeleton rows by default`() {
            buildTestPage { block { load({ 1 }) { value -> row { text("Всего: $value") } } } }

            assertEquals(3, ui().findAll("ts-skel-row").size)
        }

        @Test
        fun `should draw a skeleton row as the reference list row`() {
            buildTestPage { block { load({ 1 }, skeletonRows = 2) { value -> row { text("Всего: $value") } } } }

            val row = ui().findAll("ts-skel-row")[1]
            assertTrue("ts-list-row" in row.classes())
            assertEquals(setOf("ts-skel", "ts-skel--circle"), row.child(0).classes())
            assertEquals("28px", row.child(0).element.style.get("height"))
            val lines = row.child(1)
            assertEquals(setOf("ts-skel-row__lines"), lines.classes())
            assertEquals(listOf("80%", "30%"), lines.children.toList().map { bar -> bar.element.style.get("width") })
            assertEquals(listOf("10px", "8px"), lines.children.toList().map { bar -> bar.element.style.get("height") })
            assertEquals("10px", row.child(2).element.style.get("height"))
            assertEquals(setOf("ts-skel", "ts-skel--badge"), row.child(3).classes())
            assertEquals("22px", row.child(3).element.style.get("height"))
        }

        @Test
        fun `should hide the skeleton from assistive technologies and mark the body busy`() {
            buildTestPage { block { load({ 1 }) { value -> row { text("Всего: $value") } } } }

            assertEquals("true", ui().findAll("ts-skel-row").first().parent.orElseThrow().element.getAttribute("aria-hidden"))
            assertEquals("true", body().element.getAttribute("aria-busy"))
        }

        @Test
        fun `should show the skeleton in a flush body that is not a grid`() {
            buildTestPage { block { load({ 1 }) { value -> row { text("Всего: $value") } } } }

            assertTrue("ts-block__body--flush" in body().classes())
            assertFalse("ts-block__body--grid" in body().classes())
        }

        @Test
        fun `should show the block head and actions while loading`() {
            buildTestPage {
                block(title = "Туры") {
                    load({ 1 }) { value -> row { text("Всего: $value") } }
                    actions { action("Создать") }
                }
            }

            assertEquals("Туры", ui().find("ts-block__title").element.text)
            assertTrue(button("Создать").isVisible)
        }

        @Test
        fun `should hide the footer without own content while loading`() {
            buildTestPage { block { load({ 1 }) { value -> row { text("Всего: $value") } } } }

            assertFalse(ui().find("ts-block__foot").isVisible)
        }
    }

    @Nested
    inner class ContentTests {
        @Test
        fun `should build the content with the fetched value`() {
            buildTestPage { block { load({ 42 }) { value -> row { text("Всего: $value") } } } }

            queue.runAll()

            assertEquals("Всего: 42", ui().find("ts-block__row").element.textRecursively)
            assertTrue(ui().findAll("ts-skel-row").isEmpty())
        }

        @Test
        fun `should remove the busy mark after loading`() {
            buildTestPage { block { load({ 42 }) { value -> row { text("Всего: $value") } } } }

            queue.runAll()

            assertNull(body().element.getAttribute("aria-busy"))
        }

        @Test
        fun `should build rows as a block without load does`() {
            val expected = buildTestPage { block { row { text("Всего: 3") } } }.find("ts-block__body").element.outerHTML
            val loaded = buildTestPage { block { load({ 3 }) { value -> row { text("Всего: $value") } } } }

            queue.runAll()

            assertEquals(expected, loaded.find("ts-block__body").element.outerHTML)
        }

        @Test
        fun `should build an empty state as a block without load does`() {
            val expected = buildTestPage { block { emptyState("Пусто") } }.find("ts-block__body").element.outerHTML
            val loaded = buildTestPage { block { load({ "Пусто" }) { title -> emptyState(title) } } }

            queue.runAll()

            assertEquals(expected, loaded.find("ts-block__body").element.outerHTML)
        }

        @Test
        fun `should start the fetch in the background executor`() {
            var fetches = 0
            buildTestPage { block { load({ ++fetches }) { value -> row { text("Всего: $value") } } } }

            assertEquals(0, fetches)
            assertEquals(1, queue.size)
        }

        @Test
        fun `should keep fields in loaded content following the block edit mode`() {
            lateinit var handle: BlockHandle
            buildTestPage { handle = block { load({ "Логин" }) { label -> row { textInput(label, labelSize = 4, size = 20) } } } }
            queue.runAll()

            handle.isEditable = false

            assertTrue(control<TextField>("Логин").isReadOnly)
        }

        @Test
        fun `should keep fields following the block edit mode after a reload`() {
            lateinit var block: BlockHandle
            lateinit var load: LoadHandle
            buildTestPage { block = block { load = load({ "Логин" }) { label -> row { textInput(label, labelSize = 4, size = 20) } } } }
            queue.runAll()
            load.reload()
            queue.runAll()

            block.isEditable = false

            assertTrue(control<TextField>("Логин").isReadOnly)
        }
    }

    @Nested
    inner class TableContentTests {
        @Test
        fun `should replace the complete table viewport on reload and keep pager in footer`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()
            val old = ui().find("ts-table-scroll")
            handle.reload()

            queue.runAll()

            assertFalse(old.isAttached)
            val viewport = ui().find("ts-table-scroll")
            assertTrue(viewport.isAttached)
            assertEquals(listOf(ui().find("ts-table")), viewport.children.toList())
            assertTrue(ui().find("ts-block__foot").find("ts-table-pager").isVisible)
        }

        @Test
        fun `should put the pagination of a loaded table into the block footer`() {
            buildTestPage { block { load({ (1..12).toList() }) { ids -> idTable(ids) } } }

            queue.runAll()

            val footer = ui().find("ts-block__foot")
            assertTrue(footer.isVisible)
            assertTrue("ts-table-pager" in footer.child(0).classes())
        }

        @Test
        fun `should make the body of a loaded table flush`() {
            buildTestPage { block { load({ (1..12).toList() }) { ids -> idTable(ids) } } }

            queue.runAll()

            assertTrue("ts-block__body--flush" in body().classes())
            assertFalse("ts-block__body--grid" in body().classes())
        }

        @Test
        fun `should hide the footer if the loaded table fits one page`() {
            buildTestPage { block { load({ (1..3).toList() }) { ids -> idTable(ids) } } }

            queue.runAll()

            assertFalse(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should put the pagination after own footer content`() {
            buildTestPage {
                block {
                    load({ (1..12).toList() }) { ids -> idTable(ids) }
                    footer { text("Итого") }
                }
            }

            queue.runAll()

            val footer = ui().find("ts-block__foot")
            assertEquals("Итого", footer.child(0).element.textRecursively)
            assertTrue("ts-table-pager" in footer.child(1).classes())
            assertTrue(footer.isVisible)
        }

        @Test
        fun `should remove the pagination and hide the footer on reload`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()

            handle.reload()

            assertTrue(ui().findAll("ts-table-pager").isEmpty())
            assertFalse(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should keep one pagination after a reload`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()
            handle.reload()

            queue.runAll()

            assertEquals(1, ui().findAll("ts-table-pager").size)
            assertTrue(ui().find("ts-block__foot").isVisible)
        }
    }

    @Nested
    inner class FailureTests {
        @Test
        fun `should show the error state if the fetch fails`() {
            buildTestPage { block { load<Int>({ error("Test fetch failure") }) { value -> row { text("Всего: $value") } } } }

            queue.runAll()

            val state = ui().find("ts-empty")
            assertTrue("ts-empty--error" in state.classes())
            assertEquals(testTexts.load.failed, state.find("ts-empty__title").element.text)
            assertEquals(testTexts.load.failedHint, state.find("ts-empty__desc").element.text)
            assertTrue(button(testTexts.load.retry).isVisible)
        }

        @Test
        fun `should show the error state if the content fails`() {
            buildTestPage {
                block {
                    load({ 1 }) { value ->
                        row { text("Всего: $value") }
                        error("Test content failure")
                    }
                }
            }

            queue.runAll()

            assertTrue("ts-empty--error" in ui().find("ts-empty").classes())
            assertTrue(ui().findAll("ts-block__row").isEmpty())
        }

        @Test
        fun `should draw the error state as the empty state of a block`() {
            buildTestPage { block { load<Int>({ error("Test fetch failure") }) { value -> row { text("Всего: $value") } } } }

            queue.runAll()

            assertFalse("ts-block__body--flush" in body().classes())
            assertFalse("ts-block__body--grid" in body().classes())
            assertNull(body().element.getAttribute("aria-busy"))
        }

        @Test
        fun `should show the skeleton again on retry`() {
            buildTestPage { block { load(FailingOnce()::fetch) { value -> row { text("Всего: $value") } } } }
            queue.runAll()

            button(testTexts.load.retry)._click()

            assertEquals(3, ui().findAll("ts-skel-row").size)
            assertTrue(ui().findAll("ts-empty").isEmpty())
        }

        @Test
        fun `should show the content after a successful retry`() {
            buildTestPage { block { load(FailingOnce()::fetch) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            button(testTexts.load.retry)._click()

            queue.runAll()

            assertEquals("Всего: 2", ui().find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should not turn an Error of the fetch into the error state`() {
            val executor = ThreadExecutor()
            Background.executorOverride = executor
            buildTestPage { block { load<Int>({ throw TestError() }) { value -> row { text("Всего: $value") } } } }

            executor.join()
            MockVaadin.clientRoundtrip()

            assertInstanceOf(TestError::class.java, executor.uncaught.single())
            assertTrue(ui().findAll("ts-empty").isEmpty())
            assertEquals(3, ui().findAll("ts-skel-row").size)
        }

        @Test
        fun `should let an Error of the content propagate`() {
            buildTestPage { block { load({ 1 }) { _ -> throw TestError() } } }

            assertThrows<TestError> { queue.runAll() }
            assertTrue(ui().findAll("ts-empty").isEmpty())
        }
    }

    @Nested
    inner class ReloadTests {
        @Test
        fun `should show the skeleton again on reload`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ 1 }) { value -> row { text("Всего: $value") } } } }
            queue.runAll()

            handle.reload()

            assertEquals(3, ui().findAll("ts-skel-row").size)
            assertTrue(ui().findAll("ts-block__row").isEmpty())
            assertEquals("true", body().element.getAttribute("aria-busy"))
        }

        @Test
        fun `should show the new value after a reload`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()

            handle.reload()
            queue.runAll()

            assertEquals("Всего: 2", ui().find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should reload from another thread`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            Background.executorOverride = Executor { task -> task.run() }

            thread { handle.reload() }.join()
            MockVaadin.clientRoundtrip()

            assertEquals("Всего: 2", ui().find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should apply the result of a background fetch in the UI thread`() {
            val executor = ThreadExecutor()
            Background.executorOverride = executor
            buildTestPage { block { load({ 7 }) { value -> row { text("Всего: $value") } } } }
            executor.join()

            MockVaadin.clientRoundtrip()

            assertEquals("Всего: 7", ui().find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should apply only the result of the latest reload`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            handle.reload()
            handle.reload()

            queue.runLatestFirst()

            assertEquals("Всего: 2", ui().find("ts-block__row").element.textRecursively)
            assertEquals(3, source.calls)
        }
    }

    @Nested
    inner class DetachTests {
        @Test
        fun `should drop the result if the UI was detached before the fetch finished`() {
            val source = Counter()
            buildTestPage { block { load(source::next) { value -> row { text("Всего: $value") } } } }
            val ui = UI.getCurrent()
            ui.session.removeUI(ui)

            assertDoesNotThrow { queue.runAll() }
            assertEquals(1, source.calls)
            assertTrue(ui.findAll("ts-block__row").isEmpty())
        }

        @Test
        fun `should drop the queued result if the UI is detached before it runs`() {
            val executor = ThreadExecutor()
            Background.executorOverride = executor
            buildTestPage { block { load({ 7 }) { value -> row { text("Всего: $value") } } } }
            executor.join()
            val ui = UI.getCurrent()
            val session = ui.session
            session.removeUI(ui)

            assertDoesNotThrow { session.service.runPendingAccessTasks(session) }
            assertTrue(ui.findAll("ts-block__row").isEmpty())
        }
    }

    @Nested
    inner class LeftPageTests {
        @Test
        fun `should not fetch on a reload from another thread after the page was left`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            val page = buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            leave(page)

            thread { handle.reload() }.join()

            assertDoesNotThrow { MockVaadin.clientRoundtrip() }
            assertEquals(0, queue.size)
            assertEquals(1, source.calls)
        }

        @Test
        fun `should not fetch on a reload in the UI thread after the page was left`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            val page = buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            leave(page)

            handle.reload()

            assertEquals(0, queue.size)
            assertEquals("Всего: 1", page.find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should drop the pending result after the page was left`() {
            val source = Counter()
            val page = buildTestPage { block { load(source::next) { value -> row { text("Всего: $value") } } } }
            leave(page)

            assertDoesNotThrow { queue.runAll() }

            assertTrue(page.findAll("ts-block__row").isEmpty())
            assertEquals(3, page.findAll("ts-skel-row").size)
        }

        @Test
        fun `should load again when the page with a dropped result is shown again`() {
            val source = Counter()
            val page = buildTestPage { block { load(source::next) { value -> row { text("Всего: $value") } } } }
            leave(page)
            queue.runAll()

            comeBack(page)
            queue.runAll()

            assertEquals("Всего: 2", page.find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should show the skeleton when the page is shown again after a skipped reload`() {
            lateinit var handle: LoadHandle
            val page = buildTestPage { block { handle = load({ 1 }) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            leave(page)
            handle.reload()

            comeBack(page)

            assertEquals(3, page.findAll("ts-skel-row").size)
            assertEquals(1, queue.size)
        }

        @Test
        fun `should load again when the page is shown again after a skipped reload`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            val page = buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            leave(page)
            handle.reload()

            comeBack(page)
            queue.runAll()

            assertEquals("Всего: 2", page.find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should not load again when the page is shown again without missed loads`() {
            val source = Counter()
            val page = buildTestPage { block { load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            leave(page)

            comeBack(page)

            assertEquals(0, queue.size)
            assertEquals("Всего: 1", page.find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should reload after the page is shown again`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            val page = buildTestPage { block { handle = load(source::next) { value -> row { text("Всего: $value") } } } }
            queue.runAll()
            leave(page)
            comeBack(page)

            thread { handle.reload() }.join()
            MockVaadin.clientRoundtrip()
            queue.runAll()

            assertEquals("Всего: 2", page.find("ts-block__row").element.textRecursively)
        }

        /** Takes the page built by `buildTestPage` out of the UI, as navigation to another route does. */
        private fun leave(page: Component) {
            UI.getCurrent().remove(page.parent.orElseThrow())
        }

        /** Puts the page taken out by [leave] back into the UI, as navigation back to a kept view does. */
        private fun comeBack(page: Component) {
            UI.getCurrent().add(page.parent.orElseThrow())
        }
    }

    @Nested
    inner class EmptyContentTests {
        @Test
        fun `should hide the body if the content added nothing`() {
            buildTestPage { block { load({ emptyList<Int>() }) { ids -> ids.forEach { id -> row { text("$id") } } } } }

            queue.runAll()

            assertFalse(body().isVisible)
        }

        @Test
        fun `should show the body again on reload`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ emptyList<Int>() }) { ids -> ids.forEach { id -> row { text("$id") } } } } }
            queue.runAll()

            handle.reload()

            assertTrue(body().isVisible)
            assertEquals(3, ui().findAll("ts-skel-row").size)
        }

        @Test
        fun `should show the body with content after a reload`() {
            val source = Counter()
            lateinit var handle: LoadHandle
            buildTestPage {
                block { handle = load({ List(source.next() - 1) { id -> id } }) { ids -> ids.forEach { id -> row { text("$id") } } } }
            }
            queue.runAll()

            handle.reload()
            queue.runAll()

            assertTrue(body().isVisible)
            assertEquals("0", ui().find("ts-block__row").element.textRecursively)
        }

        @Test
        fun `should keep the body hidden if the content added nothing when the load is shown`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ emptyList<Int>() }) { ids -> ids.forEach { id -> row { text("$id") } } } } }
            queue.runAll()
            handle.isVisible = false

            handle.isVisible = true

            assertFalse(body().isVisible)
        }

        @Test
        fun `should keep the body hidden while the load is hidden`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ 1 }) { value -> row { text("Всего: $value") } } } }
            handle.isVisible = false

            queue.runAll()

            assertFalse(body().isVisible)
        }
    }

    @Nested
    inner class PlacementTests {
        @Test
        fun `should reject a load in a block with rows`() {
            assertThrows<IllegalStateException> {
                buildTestPage {
                    block {
                        row { text("Строка") }
                        load({ 1 }) { value -> row { text("Всего: $value") } }
                    }
                }
            }
        }

        @Test
        fun `should reject rows after a load`() {
            assertThrows<IllegalStateException> {
                buildTestPage {
                    block {
                        load({ 1 }) { value -> row { text("Всего: $value") } }
                        row { text("Строка") }
                    }
                }
            }
        }

        @Test
        fun `should reject a table after a load`() {
            assertThrows<IllegalStateException> {
                buildTestPage {
                    block {
                        load({ 1 }) { value -> row { text("Всего: $value") } }
                        idTable(listOf(1))
                    }
                }
            }
        }

        @Test
        fun `should reject a load in a block with an empty state`() {
            assertThrows<IllegalStateException> {
                buildTestPage {
                    block {
                        emptyState("Пусто")
                        load({ 1 }) { value -> row { text("Всего: $value") } }
                    }
                }
            }
        }

        @Test
        fun `should reject a second load`() {
            assertThrows<IllegalStateException> {
                buildTestPage {
                    block {
                        load({ 1 }) { value -> row { text("Всего: $value") } }
                        load({ 2 }) { value -> row { text("Всего: $value") } }
                    }
                }
            }
        }

        @Test
        fun `should reject fewer than one skeleton row`() {
            assertThrows<IllegalArgumentException> {
                buildTestPage { block { load({ 1 }, skeletonRows = 0) { value -> row { text("Всего: $value") } } } }
            }
        }

        @Test
        fun `should reject head actions in the content`() {
            var failure: IllegalStateException? = null
            buildTestPage { block { load({ 1 }) { _ -> failure = assertThrows { actions { text("Шапка") } } } } }

            queue.runAll()

            assertEquals("Content of a load fills the block body only; call actions { } on the block itself", failure?.message)
        }

        @Test
        fun `should reject a footer in the content`() {
            var failure: IllegalStateException? = null
            buildTestPage { block { load({ 1 }) { _ -> failure = assertThrows { footer { text("Подвал") } } } } }

            queue.runAll()

            assertEquals("Content of a load fills the block body only; call footer { } on the block itself", failure?.message)
        }

        @Test
        fun `should reject an editing switch in the content`() {
            var failure: IllegalStateException? = null
            buildTestPage { block { load({ 1 }) { _ -> failure = assertThrows { editing(onSave = { true }, onCancel = {}) } } } }

            queue.runAll()

            assertEquals("Content of a load fills the block body only; call editing() on the block itself", failure?.message)
        }

        @Test
        fun `should reject tabs in the content`() {
            var failure: IllegalStateException? = null
            buildTestPage {
                block {
                    load({ 1 }) { _ ->
                        failure = assertThrows {
                            tabs(initial = 1) {
                                tab(1, "Первая")
                                tab(2, "Вторая")
                            }
                        }
                    }
                }
            }

            queue.runAll()

            assertEquals("Content of a load fills the block body only; call tabs() on the block itself", failure?.message)
        }

        @Test
        fun `should reject a load in the content`() {
            var failure: IllegalStateException? = null
            buildTestPage { block { load({ 1 }) { _ -> failure = assertThrows { load({ 2 }) { value -> row { text("$value") } } } } } }

            queue.runAll()

            assertEquals("Content of a load fills the block body only; call load() on the block itself", failure?.message)
        }
    }

    @Nested
    inner class VisibilityTests {
        @Test
        fun `should hide the loaded body through the handle`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ 1 }) { value -> row { text("Всего: $value") } } } }

            handle.isVisible = false

            assertFalse(body().isVisible)
            assertTrue(ui().find("ts-block").isVisible)
        }

        @Test
        fun `should follow a visibility signal`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ 1 }) { value -> row { text("Всего: $value") } } } }
            val signal = ValueSignal(true)
            handle.bindVisible(signal)

            signal.set(false)

            assertFalse(handle.isVisible)
            assertFalse(body().isVisible)
        }

        @Test
        fun `should hide the pagination of a loaded table with the body`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()

            handle.isVisible = false

            assertFalse(body().isVisible)
            assertTrue(ui().findAll("ts-table-pager").isEmpty())
            assertFalse(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should keep own footer content visible while the load is hidden`() {
            lateinit var handle: LoadHandle
            buildTestPage {
                block {
                    handle = load({ (1..12).toList() }) { ids -> idTable(ids) }
                    footer { text("Итого") }
                }
            }
            queue.runAll()

            handle.isVisible = false

            val footer = ui().find("ts-block__foot")
            assertTrue(footer.isVisible)
            assertEquals(listOf("Итого"), footer.children.toList().map { child -> child.element.textRecursively })
        }

        @Test
        fun `should show the pagination again when the load is shown`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()
            handle.isVisible = false

            handle.isVisible = true

            assertTrue(body().isVisible)
            assertEquals(1, ui().findAll("ts-table-pager").size)
            assertTrue(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should keep the body and the pagination hidden after a reload`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()
            handle.isVisible = false
            handle.reload()

            queue.runAll()

            assertFalse(body().isVisible)
            assertTrue(ui().findAll("ts-table-pager").isEmpty())
            assertFalse(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should hide the pagination of a table loaded while the load is hidden`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            handle.isVisible = false

            queue.runAll()

            assertTrue(ui().findAll("ts-table-pager").isEmpty())
            assertFalse(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should follow a visibility signal across a reload`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ (1..12).toList() }) { ids -> idTable(ids) } } }
            queue.runAll()
            val signal = ValueSignal(false)
            handle.bindVisible(signal)
            handle.reload()
            queue.runAll()

            signal.set(true)

            assertTrue(body().isVisible)
            assertEquals(1, ui().findAll("ts-table-pager").size)
            assertTrue(ui().find("ts-block__foot").isVisible)
        }

        @Test
        fun `should reject a manual visibility while bound`() {
            lateinit var handle: LoadHandle
            buildTestPage { block { handle = load({ 1 }) { value -> row { text("Всего: $value") } } } }
            handle.bindVisible(ValueSignal(true))

            assertThrows<BindingActiveException> { handle.isVisible = false }
        }
    }

    private fun BlockScope.idTable(ids: List<Int>) {
        table(key = { id -> id }, pageSize = 5, fetch = { request -> Page(ids.drop(request.offset).take(request.limit), ids.size) }) {
            codeColumn("ID") { id -> id.toString() }
        }
    }

    private fun ui(): Component = UI.getCurrent()

    private fun body(): Component = ui().find("ts-block__body")

    /** Keeps background tasks until a test runs them in the test thread, which is the UI thread. */
    private class QueueExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()

        val size: Int
            get() = tasks.size

        override fun execute(command: Runnable) {
            tasks += command
        }

        /** Runs the kept tasks in order, including tasks they add. */
        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }

        /** Runs the kept tasks, the latest first. */
        fun runLatestFirst() {
            while (tasks.isNotEmpty()) tasks.removeLast().run()
        }
    }

    /** Runs every task in a thread of its own and keeps what the tasks threw. */
    private class ThreadExecutor : Executor {
        private val threads = mutableListOf<Thread>()
        val uncaught: MutableList<Throwable> = java.util.Collections.synchronizedList(mutableListOf())

        override fun execute(command: Runnable) {
            threads += thread(start = false) { command.run() }.apply {
                setUncaughtExceptionHandler { _, error -> uncaught += error }
                start()
            }
        }

        fun join() {
            threads.forEach(Thread::join)
        }
    }

    /** Returns the number of the call. */
    private class Counter {
        var calls = 0
            private set

        fun next(): Int = ++calls
    }

    /** Fails the first call and returns the number of the call afterwards. */
    private class FailingOnce {
        private var calls = 0

        fun fetch(): Int {
            calls++
            check(calls > 1) { "Test fetch failure" }
            return calls
        }
    }

    private class TestError : Error("Test error")
}
