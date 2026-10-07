package tech.testsys.web.components.data

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.core.Background
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.LoadHandle
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.skipWhenHidden
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockHandle
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

internal class TableFiltersHandleTests : MockVaadinTests() {
    @Test
    fun `should refresh once after publishing an applied draft`() {
        val fixture = Fixture()
        fixture.filters.isExpanded = true
        fixture.input.value = "changed"

        fixture.apply()

        assertEquals(listOf("default" to 0, "changed" to 0), fixture.requests)
        assertTrue(fixture.filters.isExpanded)
    }

    @Test
    fun `should retain the applied snapshot when draft changes and the table is refreshed manually`() {
        val fixture = Fixture()
        fixture.input.value = "draft"

        fixture.table.refresh()

        assertEquals(listOf("default" to 0, "default" to 0), fixture.requests)
    }

    @Test
    fun `should return to the first page after applying from a later page`() {
        val fixture = Fixture()
        fixture.filters.isExpanded = true
        fixture.next()
        fixture.input.value = "changed"

        fixture.apply()

        assertEquals(listOf("default" to 0, "default" to 2, "changed" to 0), fixture.requests)
    }

    @Test
    fun `should restore nonempty defaults and refresh the first page on reset`() {
        val fixture = Fixture()
        fixture.filters.isExpanded = true
        fixture.input.value = "changed"
        fixture.apply()
        fixture.next()

        fixture.reset()

        assertEquals("default", fixture.input.value)
        assertEquals("default" to 0, fixture.requests.last())
        assertEquals(4, fixture.requests.size)
    }

    @Test
    fun `should preserve the current table page when validation rejects a draft`() {
        val fixture = Fixture()
        fixture.filters.isExpanded = true
        fixture.next()
        fixture.input.value = ""

        fixture.apply()

        assertEquals(listOf("default" to 0, "default" to 2), fixture.requests)
        assertTrue(fixture.input.isInvalid)
        assertTrue(fixture.filters.isExpanded)
    }

    @Test
    fun `should retain draft and errors when collapsing without disabling hidden binding validation`() {
        val fixture = Fixture()
        fixture.filters.isExpanded = true
        fixture.input.value = ""
        fixture.apply()

        fixture.filters.isExpanded = false

        assertEquals("", fixture.input.value)
        assertTrue(fixture.input.isInvalid)
        assertFalse(fixture.binder.writeBeanIfValid(DraftBuilder()))
        assertTrue(fixture.input.isVisible)
    }

    @Test
    fun `should keep filter fields editable independently from the block and table visibility`() {
        val fixture = Fixture()

        fixture.block.bindEditable(com.vaadin.flow.signals.local.ValueSignal(false))
        fixture.table.isVisible = false

        assertFalse(fixture.input.isReadOnly)
        assertTrue(fixture.filters.isVisible)
    }

    @Test
    fun `should start collapsed with a closed disclosure and a hidden panel`() {
        val fixture = Fixture()

        assertFalse(fixture.filters.isExpanded)
        assertEquals("false", fixture.root.find("ts-table-filters__toggle").element.getAttribute("aria-expanded"))
        assertTrue(fixture.root.find("ts-table-filters__content").element.hasAttribute("hidden"))
    }

    @Test
    fun `should expand the panel on a toggle click`() {
        val fixture = Fixture()
        val toggle = fixture.root.find("ts-table-filters__toggle")

        assertInstanceOf(NativeButton::class.java, toggle)._click()

        val content = fixture.root.find("ts-table-filters__content")
        assertTrue(fixture.filters.isExpanded)
        assertEquals("true", toggle.element.getAttribute("aria-expanded"))
        assertEquals(content.id.orElseThrow(), toggle.element.getAttribute("aria-controls"))
        assertFalse(content.element.hasAttribute("hidden"))
    }

    @Test
    fun `should put filters outside the flush table body regardless of declaration order`() {
        val fixture = Fixture()

        assertTrue("ts-table-filters" in fixture.root.child(0).classes())
        assertTrue("ts-block__body" in fixture.root.child(1).classes())
        assertTrue("ts-block__foot" in fixture.root.child(2).classes())
    }

    @Test
    fun `should reject a repeated filter panel`() {
        assertThrows<IllegalStateException> {
            buildTestPage {
                block {
                    filters(onApply = { true }, onReset = {}, onRefresh = {}) {}
                    filters(onApply = { true }, onReset = {}, onRefresh = {}) {}
                }
            }
        }
    }

    @Test
    fun `should enforce the block column capacity using ordinary field placement`() {
        assertThrows<IllegalArgumentException> {
            buildTestPage {
                row {
                    slot(size = 12) {
                        row {
                            block {
                                filters(onApply = { true }, onReset = {}, onRefresh = {}) {
                                    row { textInput("Too wide", labelSize = 4, size = 21) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    inner class LoadIntegrationTests {
        private val queue = QueueExecutor()

        @BeforeEach
        fun installQueue() {
            Background.executorOverride = queue
        }

        @AfterEach
        fun removeQueue() {
            Background.executorOverride = null
        }

        @Test
        fun `should retain filter fields and their draft when replacing loaded content`() {
            lateinit var loaded: LoadHandle
            lateinit var input: ValueInput<String>
            lateinit var filters: TableFiltersHandle
            val main = buildTestPage {
                block {
                    loaded = load({ 1 }) { value -> row { text(value.toString()) } }
                    filters = filters(onApply = { true }, onReset = {}, onRefresh = { loaded.reload() }) {
                        row { input = textInput("Draft", labelSize = 4, size = 20) }
                    }
                }
            }
            queue.runAll()
            input.value = "kept"
            filters.isExpanded = true

            loaded.reload()
            queue.runAll()

            assertEquals("kept", input.value)
            assertTrue(filters.isExpanded)
            assertEquals(1, main.findAll("ts-table-filters").size)
            assertFalse(main.find("ts-block__body").findAll("ts-table-filters").isNotEmpty())
        }

        @Test
        fun `should reject filter declarations inside the replaced load body`() {
            val main = buildTestPage {
                block {
                    load({ 1 }) { _ -> filters(onApply = { true }, onReset = {}, onRefresh = {}) {} }
                }
            }

            queue.runAll()

            assertTrue(main.find("ts-empty").classes().contains("ts-empty--error"))
            assertTrue(main.findAll("ts-table-filters").isEmpty())
        }
    }

    private class QueueExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) {
            tasks += command
        }
        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
            MockVaadin.clientRoundtrip()
        }
    }

    private class DraftBuilder {
        var value: String = "default"
    }

    private class Fixture {
        val binder = Binder<DraftBuilder>()
        val requests = mutableListOf<Pair<String, Int>>()
        private val applied = AtomicReference("default")
        lateinit var input: ValueInput<String>
        lateinit var filters: TableFiltersHandle
        lateinit var table: TableHandle<Int>
        lateinit var block: BlockHandle
        val root = buildTestPage {
            block = block {
                table = table(key = { value: Int -> value }, pageSize = 2, fetch = { request ->
                    requests += applied.get() to request.offset
                    Page((1..8).drop(request.offset).take(request.limit), total = 8)
                }) { column("ID") { value -> text(value.toString()) } }
                filters = filters(
                    onApply = {
                        val candidate = DraftBuilder()
                        binder.writeBeanIfValid(candidate).also { valid -> if (valid) applied.set(candidate.value) }
                    },
                    onReset = {
                        applied.set("default")
                        binder.readBean(DraftBuilder())
                    },
                    onRefresh = { table.refresh(toFirstPage = true) },
                ) {
                    row {
                        input = textInput("Query", labelSize = 4, size = 20) {
                            binder.forField(this).asRequired("Required")
                                .bind({ draft -> draft.value }, { draft, value -> draft.value = value }).skipWhenHidden()
                        }
                    }
                }
            }
        }.child(0)

        init {
            binder.readBean(DraftBuilder())
        }

        fun apply() = assertInstanceOf(Button::class.java, root.find("ts-table-filters__apply"))._click()
        fun reset() = assertInstanceOf(Button::class.java, root.find("ts-table-filters__reset"))._click()
        fun next() = assertInstanceOf(NativeButton::class.java, root.findAll("ts-pager__btn").last())._click()
    }
}
