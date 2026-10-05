package tech.testsys.web.devapp.showcase

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.expectView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.NumberField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.TagKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.Trend
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.devapp.MockSpringVaadinTests
import tech.testsys.web.devapp.error.NotFoundView

class ShowcaseViewTests {
    @Nested
    @SpringBootTest
    @ActiveProfiles("dev")
    inner class DevProfileTests : MockSpringVaadinTests() {
        @BeforeEach
        fun openShowcase() {
            UI.getCurrent().navigate("dev/showcase")
        }

        @Test
        fun `should open the showcase in the dev profile`() {
            expectView<ShowcaseView>()
            assertTrue(_find<Section>().isNotEmpty())
        }

        @Test
        fun `should show neutral numeric verdicts including zero`() {
            assertTrue("0" in shownTexts("ts-verdict--score"))
            assertTrue("87баллов" in shownTexts("ts-verdict--score"))
        }

        @ParameterizedTest
        @EnumSource(TagKind::class)
        fun `should show a tag of every kind`(kind: TagKind) {
            assertTrue(kind.name in shownTexts("ts-tag"))
        }

        @ParameterizedTest
        @EnumSource(Tone::class)
        fun `should show a badge of every tone`(tone: Tone) {
            assertTrue(tone.name in shownTexts("ts-status"))
        }

        @ParameterizedTest
        @EnumSource(FeedbackKind::class)
        fun `should show an alert of every feedback kind`(kind: FeedbackKind) {
            assertTrue("Алерт ${kind.name}" in shownTexts("ts-alert__title"))
        }

        @ParameterizedTest
        @EnumSource(FeedbackKind::class)
        fun `should show a toast action of every feedback kind`(kind: FeedbackKind) {
            assertTrue(_find<Button>().any { button -> button.text == "Тост ${kind.name}" })
        }

        @ParameterizedTest
        @EnumSource(Trend::class)
        fun `should show a stat delta of every trend`(trend: Trend) {
            assertTrue(_find<Component> { classes = "ts-stat__delta--${trend.name.lowercase()}" }.isNotEmpty())
        }

        @ParameterizedTest
        @ValueSource(strings = ["main", "neutral", "destructive", "link"])
        fun `should show an action of every role`(role: String) {
            assertTrue(_find<Button>().any { button -> button.element.getAttribute("data-ts-role") == role })
        }

        @ParameterizedTest
        @ValueSource(strings = ["sm", "md"])
        fun `should show an action of every size`(size: String) {
            assertTrue(_find<Button>().any { button -> button.element.getAttribute("data-ts-size") == size })
        }

        @ParameterizedTest
        @ValueSource(
            classes = [
                TextField::class, TextArea::class, Select::class, Checkbox::class, DatePicker::class, TimePicker::class,
                DateTimePicker::class, CustomField::class, IntegerField::class, NumberField::class,
            ],
        )
        fun `should show a control of every type`(type: Class<*>) {
            assertTrue(_find<Component>().any { component -> type.isInstance(component) })
        }

        @Test
        fun `should show an icon-only action`() {
            assertTrue(_find<Button>().any { button -> button.element.hasAttribute("data-ts-icon-only") })
        }

        @Test
        fun `should show the editing switch`() {
            assertTrue(_find<Button>().any { button -> button.text == "Изменить" })
        }

        @Test
        fun `should show a monospace code input`() {
            assertTrue(_find<TextField>().any { field -> field.element.hasAttribute("data-ts-mono") })
        }

        @Test
        fun `should show a counter of every kind`() {
            assertTrue(_find<Component> { classes = "ts-counter" }.size >= CounterKind.entries.size)
        }

        @Test
        fun `should show a paged table`() {
            assertTrue(_find<Table> { classes = "ts-table" }.isNotEmpty())
        }

        @Test
        fun `should show a lookup field`() {
            assertTrue(_find<CustomField<*>>().any { field -> field._find<Component> { classes = "ts-lookup" }.isNotEmpty() })
        }

        @Test
        fun `should allocate the submission table including selection on the common grid`() {
            val table = block("Посылки")._get<Table>()
            val group = table.element.children.toList().single { child -> child.tag == "colgroup" }

            assertEquals(7, group.childCount)
            assertEquals("24", table.element.style.get("--ts-table-used"))
            assertTrue(table.element.classList.contains("ts-table-grid"))
        }

        @Test
        fun `should show a verdict select in the submissions head`() {
            assertEquals("Вердикт", blockActions("Посылки")._get<Select<*>>().ariaLabel.orElseThrow())
        }

        @Test
        fun `should offer all verdicts as the empty item of the verdict select`() {
            val select = blockActions("Посылки")._get<Select<*>>()

            assertTrue(select.isEmptySelectionAllowed)
            assertEquals("Все вердикты", select.emptySelectionCaption)
        }

        @Test
        fun `should show an errors filter chip in the submissions head`() {
            assertEquals("С ошибками", blockActions("Посылки")._get<NativeButton> { classes = "ts-filter" }.text)
        }

        @Test
        fun `should count no selected submissions right after navigation`() {
            assertTrue(blockActions("Посылки")._find<Div>().any { text -> text.text == "Выбрано: 0" })
        }

        @Test
        fun `should disable the recheck action while no submission is selected`() {
            assertFalse(blockActions("Посылки")._get<Button> { text = "Перепроверить" }.isEnabled)
        }

        @Test
        fun `should show a lookup of several tasks`() {
            val fields = block("Лукап")._find<CustomField<*>>()

            assertTrue(fields.any { field -> field._find<Component> { classes = "ts-lookup--many" }.isNotEmpty() })
        }

        @ParameterizedTest
        @ValueSource(strings = ["Подтвердить", "Удалить тур", "Удалить с вводом названия", "Новый тур"])
        fun `should open a dialog from the dialog action`(text: String) {
            _get<Button> { this.text = text }._click()

            MockVaadin.clientRoundtrip()
            assertTrue(_get<Dialog>().isOpened)
        }

        @Test
        fun `should keep the new tour dialog open if the tour has no name`() {
            _get<Button> { text = "Новый тур" }._click()
            MockVaadin.clientRoundtrip()

            _get<Button> { text = "Создать" }._click()

            MockVaadin.clientRoundtrip()
            assertTrue(_get<Dialog>().isOpened)
        }

        @Test
        fun `should mark the components tab in the showcase head`() {
            val tab = _find<RouterLink> { classes = "ts-tab" }.single { link -> link.text.startsWith("Компоненты") }

            assertTrue("ts-tab--active" in tab.element.classList)
        }

        private fun shownTexts(cssClass: String): List<String> =
            _find<Component> { classes = cssClass }.map { component -> component.element.textRecursively }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("dev")
    inner class StatesPageTests : MockSpringVaadinTests() {
        @BeforeEach
        fun openStates() {
            UI.getCurrent().navigate("dev/showcase/states")
        }

        @Test
        fun `should open the states page in the dev profile`() {
            expectView<ShowcaseStatesView>()
            assertTrue(_find<Component> { classes = "ts-page-head" }.isNotEmpty())
        }

        @ParameterizedTest
        @ValueSource(strings = ["ts-tabs", "ts-pills", "ts-empty", "ts-empty--error", "ts-block__head--tabs"])
        fun `should show every navigation and state element`(cssClass: String) {
            assertTrue(_find<Component> { classes = cssClass }.isNotEmpty())
        }

        @Test
        fun `should load the failing table on retry`() {
            _get<Button> { text = "Повторить" }._click()

            assertTrue(_find<Component> { classes = "ts-empty--error" }.isEmpty())
        }

        @Test
        fun `should show a pagination of twenty pages on the first page`() {
            val buttons = block("Пагинация")._find<NativeButton> { classes = "ts-pager__btn" }

            assertEquals("1", buttons.single { button -> "ts-pager__btn--active" in button.element.classList }.text)
            assertTrue(buttons.any { button -> button.text == "20" })
        }

        @Test
        fun `should show the chosen page in the text under the pagination`() {
            block("Пагинация")._get<NativeButton> { classes = "ts-pager__btn"; text = "2" }._click()

            assertTrue(block("Пагинация")._find<Div>().any { text -> text.text == "Страница 2 из 20" })
        }

        @Test
        fun `should show menus in a block head and in table rows`() {
            assertTrue(_find<Button>().count { button -> button.element.getAttribute("aria-haspopup") == "menu" } > 2)
        }

        @Test
        fun `should show the live updates blocks`() {
            val titles = _find<Component> { classes = "ts-block__title" }.map { component -> component.element.text }

            assertTrue("Загрузка" in titles)
            assertTrue("Посылки" in titles)
            assertTrue("Часы" in titles)
        }

        @Test
        fun `should show a skeleton in the live load blocks right after navigation`() {
            assertTrue(_find<Component> { classes = "ts-skel" }.isNotEmpty())
        }

        @ParameterizedTest
        @ValueSource(strings = ["Загрузить снова", "Новая посылка через 2 с"])
        fun `should show a live updates action`(text: String) {
            assertTrue(_find<Button>().any { button -> button.text == text })
        }

        @Test
        fun `should show the current time in the clock block right after navigation`() {
            val texts = _find<Component>().map { component -> component.element.textRecursively }

            assertTrue(texts.any { text -> Regex("""\d\d:\d\d:\d\d""").matches(text) })
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("dev")
    inner class NestedPageTests : MockSpringVaadinTests() {
        @Test
        fun `should mark states section and expose compound focus examples`() {
            UI.getCurrent().navigate("dev/showcase/states/accessibility")

            expectView<ShowcaseNestedView>()
            val current = _find<RouterLink>().single { link -> link.element.getAttribute("aria-current") == "location" }
            assertEquals("dev/showcase/states", current.href)
            assertTrue(_find<DateTimePicker>().isNotEmpty())
            assertEquals(listOf("Период: с", "Период: до"), _find<DatePicker>().mapNotNull { picker -> picker.ariaLabel.orElse(null) })
            assertTrue(_find<Button>().any { button -> button.text == "Сохранить явно" })
        }
    }

    /** The block titled [title] on the open page. */
    private fun block(title: String): Section = _find<Section> { classes = "ts-block" }
        .single { section -> section._find<H3> { classes = "ts-block__title" }.any { heading -> heading.text == title } }

    /** The head actions of the block titled [title] on the open page. */
    private fun blockActions(title: String): Div = block(title)._get<Div> { classes = "ts-block__actions" }

    @Nested
    @SpringBootTest
    inner class DefaultProfileTests : MockSpringVaadinTests() {
        @Test
        fun `should not open the showcase without the dev profile`() {
            UI.getCurrent().navigate("dev/showcase")

            expectView<NotFoundView>()
        }

        @Test
        fun `should not open accessibility examples without the dev profile`() {
            UI.getCurrent().navigate("dev/showcase/states/accessibility")

            expectView<NotFoundView>()
        }

        @Test
        fun `should not open the states page without the dev profile`() {
            UI.getCurrent().navigate("dev/showcase/states")

            expectView<NotFoundView>()
        }
    }
}
