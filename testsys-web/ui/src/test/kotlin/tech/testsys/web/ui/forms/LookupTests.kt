package tech.testsys.web.ui.forms

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.control
import tech.testsys.web.ui.find
import tech.testsys.web.ui.findAll
import tech.testsys.web.ui.findAllButtons
import tech.testsys.web.ui.openDialogs
import tech.testsys.web.ui.testTexts
import tech.testsys.web.ui.data.Page
import tech.testsys.web.ui.data.PageRequest
import tech.testsys.web.ui.data.ROW_CLICK_FILTER
import tech.testsys.web.ui.data.TableScope
import tools.jackson.databind.ObjectMapper

class LookupTests : MockVaadinTests() {
    private val source = Source()

    @Test
    fun `should show the chosen value by its display text`() {
        val input = buildLookup()

        input.value = source.contests[2]

        assertEquals("Кубок 3", field().find("ts-lookup__text").element.text)
    }

    @Test
    fun `should open the lookup dialog on the open button`() {
        buildLookup()

        lookupButton(testTexts.lookup.open)._click()

        val dialog = openDialogs().single()
        assertEquals("Тур", dialog.find("ts-dialog__title").element.text)
        assertEquals(testTexts.lookup.search, dialog._find<TextField>().single().placeholder)
        assertTrue(dialog._find<TextField>().single().isAutofocus)
        assertEquals(10, rows().size)
        assertEquals("" to PageRequest(offset = 0, limit = 10, sort = null), source.requests.single())
    }

    @Test
    fun `should keep lookup search and pagination outside the table viewport`() {
        buildLookup()

        lookupButton(testTexts.lookup.open)._click()

        val dialog = openDialogs().single()
        val viewport = dialog.find("ts-table-scroll")
        assertEquals(listOf(dialog.find("ts-table")), viewport.children.toList())
        assertTrue(dialog.find("ts-dialog__foot").find("ts-table-pager").isVisible)
        assertFalse(viewport.children.toList().contains(dialog._find<TextField>().single()))
    }

    @Test
    fun `should open the lookup dialog on a click on the field`() {
        buildLookup()

        field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = false))

        assertEquals(1, openDialogs().size)
    }

    @Test
    fun `should leave clicks on the buttons inside the field to the buttons`() {
        buildLookup()

        field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = true))

        assertTrue(openDialogs().isEmpty())
    }

    @Test
    fun `should search with the typed query from the first page`() {
        buildLookup()
        lookupButton(testTexts.lookup.open)._click()
        pagerButton(testTexts.pagination.next)._click()

        searchField()._setValue(" кубок ")

        assertEquals("кубок" to PageRequest(offset = 0, limit = 10, sort = null), source.requests.last())
    }

    @Test
    fun `should choose the clicked row and close`() {
        val input = buildLookup()
        var changes = 0
        input.addValueChangeListener { changes++ }
        lookupButton(testTexts.lookup.open)._click()

        rows()[1]._fireDomEvent("click", rowClick())

        assertEquals(source.contests[1], input.value)
        assertTrue(openDialogs().isEmpty())
        assertEquals(1, changes)
        assertEquals("Турнир 2", field().find("ts-lookup__text").element.text)
    }

    @Test
    fun `should mark choosing a row as a change from the user`() {
        val input = buildLookup()
        val fromClient = mutableListOf<Boolean>()
        input.addValueChangeListener { event -> fromClient += event.isFromClient }
        lookupButton(testTexts.lookup.open)._click()

        rows()[1]._fireDomEvent("click", rowClick())

        assertEquals(listOf(true), fromClient)
    }

    @Test
    fun `should keep the value when the dialog is closed from its head`() {
        val input = buildLookup()
        input.value = source.contests[0]
        var changes = 0
        input.addValueChangeListener { changes++ }
        lookupButton(testTexts.lookup.open)._click()

        openDialogs().single()._find<Button>().single { button -> button.ariaLabel.orElse(null) == testTexts.dialog.close }._click()

        assertTrue(openDialogs().isEmpty())
        assertEquals(source.contests[0], input.value)
        assertEquals(0, changes)
    }

    @Test
    fun `should not open a second dialog while one is open`() {
        buildLookup()
        lookupButton(testTexts.lookup.open)._click()

        field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = false))

        assertEquals(1, openDialogs().size)
        assertEquals(1, source.requests.size)
    }

    @Test
    fun `should fetch pages of the given size`() {
        buildLookup(pageSize = 5)

        lookupButton(testTexts.lookup.open)._click()

        assertEquals(PageRequest(offset = 0, limit = 5, sort = null), source.requests.single().second)
        assertEquals(5, rows().size)
    }

    @Test
    fun `should highlight the current value`() {
        val input = buildLookup()
        input.value = source.contests[0]

        lookupButton(testTexts.lookup.open)._click()

        assertTrue("ts-row-selected" in rows()[0].classes())
        assertFalse("ts-row-selected" in rows()[1].classes())
    }

    @Test
    fun `should clear the value`() {
        val input = buildLookup()
        input.value = source.contests[0]

        lookupButton(testTexts.lookup.clear)._click()

        assertNull(input.value)
        assertFalse(lookupButton(testTexts.lookup.clear).isVisible)
        assertEquals("", field().find("ts-lookup__text").element.text)
    }

    @Test
    fun `should mark clearing as a change from the user`() {
        val input = buildLookup()
        input.value = source.contests[0]
        val fromClient = mutableListOf<Boolean>()
        input.addValueChangeListener { event -> fromClient += event.isFromClient }

        lookupButton(testTexts.lookup.clear)._click()

        assertEquals(listOf(true), fromClient)
    }

    @Test
    fun `should move the focus to the value button after clearing`() {
        val input = buildLookup()
        input.value = source.contests[0]
        pendingJavaScript()

        lookupButton(testTexts.lookup.clear)._click()

        val valueButton = field().find("ts-lookup__text")
        assertTrue(pendingJavaScript().any { call -> call.owner == valueButton.element.node && "focus" in call.invocation.expression })
    }

    @Test
    fun `should keep the open button out of the tab order and the clear button in it`() {
        val input = buildLookup()
        input.value = source.contests[0]

        assertEquals(-1, lookupButton(testTexts.lookup.open).tabIndex)
        assertNull(lookupButton(testTexts.lookup.clear).element.getAttribute("tabindex"))
    }

    @Test
    fun `should name the empty value button after what it does`() {
        buildLookup()

        val valueButton = field().find("ts-lookup__text").element
        assertEquals(testTexts.lookup.open, valueButton.getAttribute("aria-label"))
        assertEquals("dialog", valueButton.getAttribute("aria-haspopup"))
        assertNull(valueButton.getAttribute("aria-disabled"))
    }

    @ParameterizedTest
    @CsvSource("false, true", "true, false")
    fun `should mark the value button disabled and without a popup unless the field is editable and enabled`(
        isEditable: Boolean,
        isEnabled: Boolean,
    ) {
        val input = buildLookup()

        input.isEditable = isEditable
        input.isEnabled = isEnabled

        val valueButton = field().find("ts-lookup__text").element
        assertEquals("Тур", valueButton.getAttribute("aria-label"))
        assertNull(valueButton.getAttribute("aria-haspopup"))
        assertEquals("true", valueButton.getAttribute("aria-disabled"))
    }

    @Test
    fun `should offer the popup again when the field becomes editable`() {
        val input = buildLookup()
        input.isEditable = false

        input.isEditable = true

        val valueButton = field().find("ts-lookup__text").element
        assertEquals("dialog", valueButton.getAttribute("aria-haspopup"))
        assertNull(valueButton.getAttribute("aria-disabled"))
    }

    @Test
    fun `should hide the clear button when the value is empty`() {
        buildLookup()

        assertFalse(lookupButton(testTexts.lookup.clear).isVisible)
        assertTrue(lookupButton(testTexts.lookup.open).isVisible)
    }

    @ParameterizedTest
    @CsvSource("false, true", "true, false")
    fun `should offer neither choosing nor clearing unless the field is editable and enabled`(isEditable: Boolean, isEnabled: Boolean) {
        val input = buildLookup()
        input.value = source.contests[0]

        input.isEditable = isEditable
        input.isEnabled = isEnabled

        assertFalse(lookupButton(testTexts.lookup.clear).isVisible)
        assertFalse(lookupButton(testTexts.lookup.open).isVisible)
    }

    @Test
    fun `should not open the dialog on a click on a read-only field`() {
        val input = buildLookup()
        input.isEditable = false

        field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = false))

        assertTrue(openDialogs().isEmpty())
    }

    @Test
    fun `should mark the field read-only for styling when not editable`() {
        val input = buildLookup()

        input.isEditable = false

        assertTrue(field().element.hasAttribute("readonly"))
    }

    @Test
    fun `should reject a lookup without columns`() {
        assertThrows<IllegalArgumentException> {
            buildTestRow {
                lookup("Тур", labelSize = 4, size = 8, fetch = source::fetch, display = { contest -> contest.name }, columns = {})
            }
        }
    }

    @Test
    fun `should reject a page size below one`() {
        assertThrows<IllegalArgumentException> { buildLookup(pageSize = 0) }
    }

    @Test
    fun `should reject an own empty text in columns`() {
        assertThrows<IllegalArgumentException> {
            buildLookup {
                textColumn("Название") { contest -> contest.name }
                empty("Туров нет")
            }
        }
    }

    @Test
    fun `should reject an own row click in columns`() {
        assertThrows<IllegalArgumentException> {
            buildLookup {
                textColumn("Название") { contest -> contest.name }
                onRowClick {}
            }
        }
    }

    @Test
    fun `should reject a menu column in columns`() {
        assertThrows<IllegalArgumentException> {
            buildLookup {
                textColumn("Название") { contest -> contest.name }
                menuColumn { item("Открыть") {} }
            }
        }
    }

    @Test
    fun `should require a value with Binder asRequired`() {
        val input = buildLookup()
        val binder = Binder<Form>().apply {
            forField(input).asRequired("Выберите тур").bind({ form -> form.contest }, { form, value -> form.contest = value })
        }

        val status = binder.validate()

        assertFalse(status.isOk)
        assertTrue(input.isInvalid)
        assertEquals("Выберите тур", input.errorMessage)
        assertTrue(UI.getCurrent().find("ts-field__required").isVisible)
    }

    @Test
    fun `should show nothing found when the search finds no rows`() {
        buildLookup()
        lookupButton(testTexts.lookup.open)._click()

        searchField()._setValue("олимпиада")

        assertEquals(testTexts.lookup.empty, rows().single().element.textRecursively)
    }

    @Test
    fun `should show the pager in the dialog footer`() {
        buildLookup()

        lookupButton(testTexts.lookup.open)._click()

        val foot = openDialogs().single().find("ts-dialog__foot")
        assertTrue(foot.isVisible)
        assertEquals(1, foot.findAll("ts-table-pager").size)
    }

    @Test
    fun `should show the load failure in the dialog if the first fetch fails`() {
        buildLookup()
        source.failures = 1

        lookupButton(testTexts.lookup.open)._click()

        val state = openDialogs().single().find("ts-empty")
        assertTrue("ts-empty--error" in state.classes())
        assertEquals(testTexts.load.failed, state.find("ts-empty__title").element.text)
    }

    @Test
    fun `should load rows with the pager on retry after the first fetch failed`() {
        buildLookup()
        source.failures = 1
        lookupButton(testTexts.lookup.open)._click()
        val dialog = openDialogs().single()

        findAllButtons(dialog).single { button -> button.text == testTexts.load.retry }._click()

        assertEquals(10, rows().size)
        assertTrue(dialog.find("ts-dialog__foot").isVisible)
        assertTrue(dialog.find("ts-table-pager").isVisible)
    }

    @Test
    fun `should name the control after its label`() {
        buildLookup()

        assertEquals("Тур", field().element.getProperty("accessibleName"))
    }

    private fun buildLookup(
        pageSize: Int = LOOKUP_PAGE_SIZE,
        columns: TableScope<Contest>.() -> Unit = { textColumn("Название") { contest -> contest.name } },
    ): ValueInput<Contest?> {
        lateinit var input: ValueInput<Contest?>
        buildTestRow {
            input = lookup(
                "Тур",
                labelSize = 4,
                size = 8,
                fetch = source::fetch,
                display = { contest -> contest.name },
                columns = columns,
                pageSize = pageSize,
            )
        }
        return input
    }

    private fun field(): CustomField<*> = control<CustomField<*>>("Тур")

    /** The icon button of the field named [label], shown or hidden. */
    private fun lookupButton(label: String): Button =
        field().find("ts-lookup").children.toList().filterIsInstance<Button>().single { button -> button.ariaLabel.orElse(null) == label }

    private fun searchField(): TextField = openDialogs().single()._find<TextField>().single()

    /** Rows of the table body in the open dialog. */
    private fun rows(): List<Component> = openDialogs().single().find("ts-table").children.toList()
        .filterIsInstance<TableBody>().single()
        .children.toList().filterIsInstance<TableRow>()

    /**
     * Takes the JavaScript calls queued since the last call, flushing the queue as the response would do;
     * Karibu `_find` between the steps of a test would consume it.
     */
    private fun pendingJavaScript(): List<PendingJavaScriptInvocation> {
        val internals = UI.getCurrent().internals
        internals.stateTree.runExecutionsBeforeClientResponse()
        return internals.dumpPendingJavaScriptInvocations()
    }

    private fun pagerButton(label: String): NativeButton = openDialogs().single().findAll("ts-pager__btn")
        .filterIsInstance<NativeButton>()
        .single { button -> button.element.getAttribute("aria-label") == label }

    /** Data of a row click as the client sends it, passing the filter of clicks on controls. */
    private fun rowClick() = ObjectMapper().createObjectNode().put(ROW_CLICK_FILTER, true)

    /** Data of a click on the field as the client sends it: whether the click passed the filter of button clicks. */
    private fun fieldClick(isOnButton: Boolean) = ObjectMapper().createObjectNode().put(LOOKUP_CLICK_FILTER, !isOnButton)

    private data class Contest(val id: Int, val name: String)

    private class Form(var contest: Contest? = null)

    /** Thirty contests in memory, cups at odd ids; records every query and request; the next [failures] fetches throw. */
    private class Source {
        val contests = (1..30).map { id -> Contest(id, if (id % 2 == 1) "Кубок $id" else "Турнир $id") }
        val requests = mutableListOf<Pair<String, PageRequest>>()
        var failures = 0

        fun fetch(query: String, request: PageRequest): Page<Contest> {
            requests += query to request
            if (failures > 0) {
                failures--
                error("Test fetch failure")
            }
            val found = contests.filter { contest -> contest.name.contains(query, ignoreCase = true) }
            return Page(found.drop(request.offset).take(request.limit), found.size)
        }
    }
}
