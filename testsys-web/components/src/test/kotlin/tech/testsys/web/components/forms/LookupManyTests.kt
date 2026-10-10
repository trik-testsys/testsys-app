package tech.testsys.web.components.forms

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableHead
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.value.ValueChangeMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.classes
import tech.testsys.web.components.control
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.ROW_CLICK_FILTER
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.find
import tech.testsys.web.components.findAll
import tech.testsys.web.components.findAllButtons
import tech.testsys.web.components.openDialogs
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.testTexts
import tools.jackson.databind.ObjectMapper

class LookupManyTests : MockVaadinTests() {
    private val source = Source()

    @ParameterizedTest
    @CsvSource("false,true", "true,false")
    fun `should close an open lookup when choice becomes unavailable`(enabled: Boolean, editable: Boolean) {
        val input = buildLookupMany()
        lookupButton(testTexts.lookup.open)._click()
        rows()[1]._fireDomEvent("click", rowClick())

        input.isEnabled = enabled
        input.isEditable = editable

        assertTrue(openDialogs().isEmpty())
    }

    @ParameterizedTest
    @CsvSource("false,true", "true,false")
    fun `should ignore a delayed apply after the lookup becomes unavailable`(enabled: Boolean, editable: Boolean) {
        val input = buildLookupMany()
        val form = Form()
        Binder<Form>().apply {
            forField(input).bind({ bean -> bean.contests }, { bean, value -> bean.contests = value })
            setBean(form)
        }
        var changes = 0
        input.addValueChangeListener { changes++ }
        lookupButton(testTexts.lookup.open)._click()
        rows()[1]._fireDomEvent("click", rowClick())
        val apply = dialogButton(testTexts.lookup.apply)
        input.isEnabled = enabled
        input.isEditable = editable

        apply.click()

        assertEquals(emptySet<Contest>(), input.value)
        assertEquals(0, changes)
        assertEquals(emptySet<Contest>(), form.contests)
    }

    @Nested
    inner class FieldTests {
        @Test
        fun `should start with the empty set as the value`() {
            val input = buildLookupMany()

            assertEquals(emptySet<Contest>(), input.value)
            assertEquals(emptySet<Contest>(), input.emptyValue)
        }

        @Test
        fun `should show no chips and no clear button when the value is empty`() {
            buildLookupMany()

            assertTrue(field().findAll("ts-chip").isEmpty())
            assertFalse(lookupButton(testTexts.lookup.clear).isVisible)
            assertTrue(lookupButton(testTexts.lookup.open).isVisible)
        }

        @Test
        fun `should show a chip with the display text of each value`() {
            val input = buildLookupMany()

            input.value = setOf(source.contests[0], source.contests[1])

            assertEquals(listOf("Кубок 1", "Турнир 2"), chips().map { chip -> chip.element.textRecursively })
            assertTrue(field().findAll("ts-chip--more").isEmpty())
            assertTrue(lookupButton(testTexts.lookup.clear).isVisible)
        }

        @Test
        fun `should show three chips and the count of the rest when the value has more`() {
            val input = buildLookupMany()

            input.value = source.contests.take(5).toSet()

            assertEquals(listOf("Кубок 1", "Турнир 2", "Кубок 3"), chips().map { chip -> chip.element.textRecursively })
            assertEquals("+2", field().find("ts-chip--more").element.text)
        }

        @Test
        fun `should name the remove button of a chip after its value`() {
            val input = buildLookupMany()

            input.value = setOf(source.contests[0])

            assertEquals(testTexts.lookup.remove("Кубок 1"), removeButtons().single().element.getAttribute("aria-label"))
        }

        @Test
        fun `should remove the value of a chip on its remove button`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0], source.contests[1])
            val fromClient = mutableListOf<Boolean>()
            input.addValueChangeListener { event -> fromClient += event.isFromClient }

            removeButtons()[0]._click()

            assertEquals(setOf(source.contests[1]), input.value)
            assertEquals(listOf(true), fromClient)
            assertEquals(listOf("Турнир 2"), chips().map { chip -> chip.element.textRecursively })
        }

        @Test
        fun `should leave clicks on a chip remove button out of the click on the field`() {
            val input = buildLookupMany()

            input.value = setOf(source.contests[0])

            assertTrue(isOwnClickTarget(removeButtons().single()))
        }

        @Test
        fun `should move the focus to the value button after removing a chip`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0], source.contests[1])
            pendingJavaScript()

            removeButtons()[0]._click()

            val valueButton = field().find("ts-lookup__text")
            assertTrue(pendingJavaScript().any { call -> call.owner == valueButton.element.node && "focus" in call.invocation.expression })
        }

        @Test
        fun `should move the focus to the value button after clearing`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0])
            pendingJavaScript()

            lookupButton(testTexts.lookup.clear)._click()

            val valueButton = field().find("ts-lookup__text")
            assertTrue(pendingJavaScript().any { call -> call.owner == valueButton.element.node && "focus" in call.invocation.expression })
        }

        @Test
        fun `should clear the value`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0], source.contests[1])
            val fromClient = mutableListOf<Boolean>()
            input.addValueChangeListener { event -> fromClient += event.isFromClient }

            lookupButton(testTexts.lookup.clear)._click()

            assertEquals(emptySet<Contest>(), input.value)
            assertEquals(listOf(true), fromClient)
            assertTrue(chips().isEmpty())
            assertFalse(lookupButton(testTexts.lookup.clear).isVisible)
        }

        @Test
        fun `should name the value button after all chosen values`() {
            val input = buildLookupMany()

            input.value = source.contests.take(4).toSet()

            val valueButton = field().find("ts-lookup__text").element
            assertEquals("Кубок 1, Турнир 2, Кубок 3, Турнир 4", valueButton.getAttribute("aria-label"))
            assertEquals("", valueButton.text)
        }

        @Test
        fun `should name the empty value button after what it does`() {
            buildLookupMany()

            val valueButton = field().find("ts-lookup__text").element
            assertEquals(testTexts.lookup.open, valueButton.getAttribute("aria-label"))
            assertEquals("dialog", valueButton.getAttribute("aria-haspopup"))
        }

        @Test
        fun `should put the value button before the chips so the field label focuses it`() {
            val input = buildLookupMany()

            input.value = setOf(source.contests[0])

            val box = field().find("ts-lookup")
            assertTrue("ts-lookup__text" in box.children.toList().first().classes())
            assertEquals(-1, lookupButton(testTexts.lookup.open).tabIndex)
        }

        @Test
        fun `should name the control after its label`() {
            buildLookupMany()

            assertEquals("Туры", field().element.getProperty("accessibleName"))
        }
    }

    @Nested
    inner class DialogTests {
        @Test
        fun `should open the dialog titled after the field on a click on the field`() {
            buildLookupMany()

            field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = false))

            val dialog = openDialogs().single()
            assertEquals("Туры", dialog.find("ts-dialog__title").element.text)
            assertEquals(10, rows().size)
            assertEquals("" to PageRequest(offset = 0, limit = 10, sort = null), source.requests.single())
        }

        @Test
        fun `should open a dialog of size M by default`() {
            buildTestRow {
                lookupMany(
                    "Туры",
                    labelSize = 4,
                    size = 8,
                    fetch = source::fetch,
                    display = { contest -> contest.name },
                    columns = { textColumn("Название") { contest -> contest.name } },
                )
            }

            openDialog()

            assertTrue("ts-dialog--md" in openDialogs().single().find("ts-dialog").classes())
        }

        @Test
        fun `should open the dialog of the given size`() {
            buildLookupMany(dialogSize = DialogSize.L)

            openDialog()

            assertTrue("ts-dialog--lg" in openDialogs().single().find("ts-dialog").classes())
        }

        @Test
        fun `should give the checkbox two fractions of the dialog table`() {
            buildLookupMany(columns = { textColumn("Название", size = 22) { contest -> contest.name } })

            openDialog()

            assertEquals("24", openDialogs().single().find("ts-table").element.style.get("--ts-table-used"))
        }

        @Test
        fun `should not open a second dialog while one is open`() {
            buildLookupMany()
            openDialog()

            field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = false))

            assertEquals(1, openDialogs().size)
            assertEquals(1, source.requests.size)
        }

        @Test
        fun `should check the rows of the value when the dialog opens`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0], source.contests[2])

            openDialog()

            assertEquals(listOf(true, false, true, false), rows().take(4).map { row -> checkboxOf(row).value })
            assertEquals(testTexts.lookup.selectedCount(2), selectedCount())
        }

        @Test
        fun `should check an unchecked row on a click on the row`() {
            buildLookupMany()
            openDialog()

            rows()[1]._fireDomEvent("click", rowClick())

            assertTrue(checkboxOf(rows()[1]).value)
            assertTrue("ts-row-selected" in rows()[1].classes())
            assertEquals(testTexts.lookup.selectedCount(1), selectedCount())
        }

        @Test
        fun `should uncheck a checked row on a click on the row`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[1])
            openDialog()

            rows()[1]._fireDomEvent("click", rowClick())

            assertFalse(checkboxOf(rows()[1]).value)
            assertEquals(testTexts.lookup.selectedCount(0), selectedCount())
        }

        @Test
        fun `should show the header checkbox partly checked after a click on a row`() {
            buildLookupMany()
            openDialog()

            rows()[1]._fireDomEvent("click", rowClick())

            assertFalse(headerCheckbox().value)
            assertTrue(headerCheckbox().isIndeterminate)
        }

        @Test
        fun `should show the header checkbox checked when the value holds the whole page`() {
            val input = buildLookupMany()
            input.value = source.contests.take(10).toSet()

            openDialog()

            assertTrue(headerCheckbox().value)
            assertFalse(headerCheckbox().isIndeterminate)
        }

        @Test
        fun `should count the rows checked by the header checkbox`() {
            buildLookupMany()
            openDialog()

            headerCheckbox()._setValue(true)

            assertEquals(testTexts.lookup.selectedCount(10), selectedCount())
        }

        @Test
        fun `should count the rows checked by their checkboxes`() {
            buildLookupMany()
            openDialog()

            checkboxOf(rows()[0])._setValue(true)

            assertEquals(testTexts.lookup.selectedCount(1), selectedCount())
        }

        @Test
        fun `should keep the value while rows are checked`() {
            val input = buildLookupMany()
            openDialog()

            rows()[0]._fireDomEvent("click", rowClick())

            assertEquals(emptySet<Contest>(), input.value)
            assertEquals(1, openDialogs().size)
        }

        @Test
        fun `should uncheck all rows on reset without changing the value`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0], source.contests[1])
            openDialog()

            dialogButton(testTexts.lookup.reset)._click()

            assertEquals(listOf(false, false), rows().take(2).map { row -> checkboxOf(row).value })
            assertEquals(testTexts.lookup.selectedCount(0), selectedCount())
            assertEquals(setOf(source.contests[0], source.contests[1]), input.value)
        }

        @Test
        fun `should make the checked rows the value on apply and close`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0])
            val fromClient = mutableListOf<Boolean>()
            input.addValueChangeListener { event -> fromClient += event.isFromClient }
            openDialog()
            rows()[2]._fireDomEvent("click", rowClick())

            dialogButton(testTexts.lookup.apply)._click()

            assertEquals(setOf(source.contests[0], source.contests[2]), input.value)
            assertEquals(listOf(true), fromClient)
            assertTrue(openDialogs().isEmpty())
            assertEquals(listOf("Кубок 1", "Кубок 3"), chips().map { chip -> chip.element.textRecursively })
        }

        @Test
        fun `should keep the value when the dialog is closed from its head`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0])
            var changes = 0
            input.addValueChangeListener { changes++ }
            openDialog()
            rows()[1]._fireDomEvent("click", rowClick())

            openDialogs().single()._find<Button>().single { button -> button.ariaLabel.orElse(null) == testTexts.dialog.close }._click()

            assertTrue(openDialogs().isEmpty())
            assertEquals(setOf(source.contests[0]), input.value)
            assertEquals(0, changes)
        }

        /** Esc and a click outside close the dialog in the browser, which reaches the server as closing it. */
        @Test
        fun `should keep the value when the dialog is closed by Esc or a click outside`() {
            val input = buildLookupMany()
            openDialog()
            rows()[1]._fireDomEvent("click", rowClick())

            openDialogs().single().close()

            assertTrue(openDialogs().isEmpty())
            assertEquals(emptySet<Contest>(), input.value)
        }

        @Test
        fun `should drop the checks that were not applied when the dialog opens again`() {
            buildLookupMany()
            openDialog()
            rows()[1]._fireDomEvent("click", rowClick())
            openDialogs().single().close()

            openDialog()

            assertFalse(checkboxOf(rows()[1]).value)
            assertEquals(testTexts.lookup.selectedCount(0), selectedCount())
        }

        @Test
        fun `should search lazily with the search delay`() {
            buildLookupMany()

            openDialog()

            assertEquals(ValueChangeMode.LAZY, searchField().valueChangeMode)
            assertEquals(300, searchField().valueChangeTimeout)
            assertTrue(searchField().isAutofocus)
        }

        @Test
        fun `should search with the typed query from the first page`() {
            buildLookupMany()
            openDialog()
            pagerButton(testTexts.pagination.next)._click()

            searchField()._setValue(" турнир ")

            assertEquals("турнир" to PageRequest(offset = 0, limit = 10, sort = null), source.requests.last())
        }

        @Test
        fun `should keep the checks when the search changes the rows`() {
            buildLookupMany()
            openDialog()
            rows()[1]._fireDomEvent("click", rowClick())

            searchField()._setValue("турнир")

            assertEquals("Турнир 2", rows()[0].element.textRecursively)
            assertTrue(checkboxOf(rows()[0]).value)
            assertEquals(testTexts.lookup.selectedCount(1), selectedCount())
        }

        @Test
        fun `should apply the checks of rows the search hides`() {
            val input = buildLookupMany()
            openDialog()
            rows()[0]._fireDomEvent("click", rowClick())
            searchField()._setValue("турнир")
            rows()[0]._fireDomEvent("click", rowClick())

            dialogButton(testTexts.lookup.apply)._click()

            assertEquals(setOf(source.contests[0], source.contests[1]), input.value)
        }

        @Test
        fun `should apply the checks of all pages`() {
            val input = buildLookupMany()
            openDialog()
            rows()[0]._fireDomEvent("click", rowClick())
            pagerButton(testTexts.pagination.next)._click()
            rows()[0]._fireDomEvent("click", rowClick())

            dialogButton(testTexts.lookup.apply)._click()

            assertEquals(setOf(source.contests[0], source.contests[10]), input.value)
        }

        @Test
        fun `should show nothing found when the search finds no rows`() {
            buildLookupMany()
            openDialog()

            searchField()._setValue("олимпиада")

            assertEquals(testTexts.lookup.empty, rows().single().element.textRecursively)
        }

        @Test
        fun `should show the count, reset and apply in the dialog footer`() {
            buildLookupMany()

            openDialog()

            val foot = openDialogs().single().find("ts-dialog__foot")
            assertTrue(foot.isVisible)
            assertEquals(1, foot.findAll("ts-lookup-count").size)
            assertEquals(listOf(testTexts.lookup.reset, testTexts.lookup.apply), findAllButtons(foot).map { button -> button.text })
        }

        @Test
        fun `should fetch pages of the given size`() {
            buildLookupMany(pageSize = 5)

            openDialog()

            assertEquals(PageRequest(offset = 0, limit = 5, sort = null), source.requests.single().second)
            assertEquals(5, rows().size)
        }
    }

    @Nested
    inner class StateTests {
        @Test
        fun `should not open the dialog on a click on a read-only field`() {
            val input = buildLookupMany()
            input.isEditable = false

            field().find("ts-lookup")._fireDomEvent("click", fieldClick(isOnButton = false))

            assertTrue(openDialogs().isEmpty())
        }

        @ParameterizedTest
        @CsvSource("false, true", "true, false")
        fun `should offer neither removing nor clearing unless the field is editable and enabled`(isEditable: Boolean, isEnabled: Boolean) {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0], source.contests[1])

            input.isEditable = isEditable
            input.isEnabled = isEnabled

            assertEquals(2, chips().size)
            assertTrue(removeButtons().isEmpty())
            assertFalse(lookupButton(testTexts.lookup.clear).isVisible)
            assertFalse(lookupButton(testTexts.lookup.open).isVisible)
        }

        @Test
        fun `should offer removing again when the field becomes editable`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[0])
            input.isEditable = false

            input.isEditable = true

            assertEquals(1, removeButtons().size)
            assertEquals("dialog", field().find("ts-lookup__text").element.getAttribute("aria-haspopup"))
        }

        @Test
        fun `should mark the field read-only for styling when not editable`() {
            val input = buildLookupMany()

            input.isEditable = false

            assertTrue(field().element.hasAttribute("readonly"))
        }
    }

    @Nested
    inner class BinderTests {
        @Test
        fun `should replace selected entities with fetched instances when their ids stay the same`() {
            val input = buildLookupMany()
            val fresh = source.contests[0]
            val old = Contest(id = fresh.id, name = "Старое название")
            val form = Form(setOf(old))
            Binder<Form>().apply {
                forField(input).bind({ bean -> bean.contests }, { bean, value -> bean.contests = value })
                setBean(form)
            }
            val fromClient = mutableListOf<Boolean>()
            input.addValueChangeListener { event -> fromClient += event.isFromClient }
            openDialog()

            dialogButton(testTexts.lookup.apply)._click()

            assertSame(fresh, input.value.single())
            assertSame(fresh, form.contests.single())
            assertEquals(listOf("Кубок 1"), chips().map { chip -> chip.element.textRecursively })
            assertEquals(listOf(true), fromClient)
            assertTrue(openDialogs().isEmpty())
        }

        @Test
        fun `should not report a value change when applying the same entity instances`() {
            val input = buildLookupMany()
            input.value = setOf(source.contests[1], source.contests[0])
            val changes = mutableListOf<Set<Contest>>()
            input.addValueChangeListener { event -> changes += event.value }
            openDialog()

            dialogButton(testTexts.lookup.apply)._click()

            assertTrue(changes.isEmpty())
            assertEquals(listOf("Турнир 2", "Кубок 1"), chips().map { chip -> chip.element.textRecursively })
        }

        @Test
        fun `should require a value with Binder asRequired`() {
            val input = buildLookupMany()
            val binder = Binder<Form>().apply {
                forField(input).asRequired("Выберите туры")
                    .bind({ form -> form.contests }, { form, value -> form.contests = value })
            }

            val status = binder.validate()

            assertFalse(status.isOk)
            assertTrue(input.isInvalid)
            assertEquals("Выберите туры", input.errorMessage)
        }

        @Test
        fun `should write the applied values to the bean`() {
            val input = buildLookupMany()
            val form = Form()
            Binder<Form>().apply {
                forField(input).bind({ bean -> bean.contests }, { bean, value -> bean.contests = value })
                setBean(form)
            }
            openDialog()
            rows()[0]._fireDomEvent("click", rowClick())

            dialogButton(testTexts.lookup.apply)._click()

            assertEquals(setOf(source.contests[0]), form.contests)
        }
    }

    @Nested
    inner class ArgumentTests {
        @Test
        fun `should reject a lookup without columns`() {
            assertThrows<IllegalArgumentException> { buildLookupMany(columns = {}) }
        }

        @Test
        fun `should reject a page size below one`() {
            assertThrows<IllegalArgumentException> { buildLookupMany(pageSize = 0) }
        }

        @Test
        fun `should reject an own empty text in columns`() {
            assertThrows<IllegalArgumentException> {
                buildLookupMany {
                    textColumn("Название") { contest -> contest.name }
                    empty("Туров нет")
                }
            }
        }

        @Test
        fun `should reject an own row click in columns`() {
            assertThrows<IllegalArgumentException> {
                buildLookupMany {
                    textColumn("Название") { contest -> contest.name }
                    onRowClick {}
                }
            }
        }

        @Test
        fun `should reject a menu column in columns`() {
            assertThrows<IllegalArgumentException> {
                buildLookupMany {
                    textColumn("Название") { contest -> contest.name }
                    menuColumn { item("Открыть") {} }
                }
            }
        }
    }

    private fun buildLookupMany(
        pageSize: Int = LOOKUP_PAGE_SIZE,
        dialogSize: DialogSize = DialogSize.M,
        columns: TableScope<Contest>.() -> Unit = { textColumn("Название") { contest -> contest.name } },
    ): ValueInput<Set<Contest>> {
        lateinit var input: ValueInput<Set<Contest>>
        buildTestRow {
            input = lookupMany(
                "Туры",
                labelSize = 4,
                size = 8,
                fetch = source::fetch,
                display = { contest -> contest.name },
                columns = columns,
                pageSize = pageSize,
                dialogSize = dialogSize,
            )
        }
        return input
    }

    private fun field(): CustomField<*> = control<CustomField<*>>("Туры")

    private fun openDialog() {
        lookupButton(testTexts.lookup.open)._click()
    }

    /** Chips of the values, without the chip of the rest. */
    private fun chips(): List<Component> = field().findAll("ts-chip").filter { chip -> "ts-chip--more" !in chip.classes() }

    private fun removeButtons(): List<NativeButton> = field().findAll("ts-chip__x").filterIsInstance<NativeButton>()

    /** The icon button of the field named [label], shown or hidden. */
    private fun lookupButton(label: String): Button =
        field().find("ts-lookup").children.toList().filterIsInstance<Button>().single { button -> button.ariaLabel.orElse(null) == label }

    private fun dialogButton(text: String): Button = findAllButtons(openDialogs().single()).single { button -> button.text == text }

    private fun searchField(): TextField = openDialogs().single()._find<TextField>().single()

    private fun selectedCount(): String = openDialogs().single().find("ts-lookup-count").element.text

    /** Rows of the table body in the open dialog. */
    private fun rows(): List<Component> = openDialogs().single().find("ts-table").children.toList()
        .filterIsInstance<TableBody>().single()
        .children.toList().filterIsInstance<TableRow>()

    private fun checkboxOf(row: Component): Checkbox = row._find<Checkbox>().single()

    /** The checkbox that checks all rows of the page in the open dialog. */
    private fun headerCheckbox(): Checkbox = openDialogs().single().find("ts-table").children.toList()
        .filterIsInstance<TableHead>().single()
        ._find<Checkbox>().single()

    /** Whether [target] matches one of [LOOKUP_OWN_CLICKS], whose clicks the click filter of the field leaves to them. */
    private fun isOwnClickTarget(target: Component): Boolean = LOOKUP_OWN_CLICKS.split(',')
        .map { selector -> selector.trim() }
        .any { selector -> if (selector.startsWith('.')) selector.drop(1) in target.classes() else selector == target.element.tag }

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

    private data class Contest(val id: Int, val name: String) {
        override fun equals(other: Any?): Boolean = other is Contest && id == other.id

        override fun hashCode(): Int = id.hashCode()
    }

    private class Form(var contests: Set<Contest> = emptySet())

    /** Thirty contests in memory, cups at odd ids; records every query and request. */
    private class Source {
        val contests = (1..30).map { id -> Contest(id, if (id % 2 == 1) "Кубок $id" else "Турнир $id") }
        val requests = mutableListOf<Pair<String, PageRequest>>()

        fun fetch(query: String, request: PageRequest): Page<Contest> {
            requests += query to request
            val found = contests.filter { contest -> contest.name.contains(query, ignoreCase = true) }
            return Page(found.drop(request.offset).take(request.limit), found.size)
        }
    }
}
