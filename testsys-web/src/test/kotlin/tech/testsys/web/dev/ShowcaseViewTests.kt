package tech.testsys.web.dev

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.NotFoundError
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
import com.vaadin.flow.component.html.NativeTable
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.NumberField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.timepicker.TimePicker
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tech.testsys.web.MockSpringVaadinTests
import tech.testsys.web.ui.display.CounterKind
import tech.testsys.web.ui.display.TagKind
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.display.Trend
import tech.testsys.web.ui.feedback.FeedbackKind

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
            assertTrue(_find<NativeTable> { classes = "ts-table" }.isNotEmpty())
        }

        @Test
        fun `should show a lookup field`() {
            assertTrue(_find<CustomField<*>>().any { field -> field._find<Component> { classes = "ts-lookup" }.isNotEmpty() })
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

        private fun shownTexts(cssClass: String): List<String> =
            _find<Component> { classes = cssClass }.map { component -> component.element.textRecursively }
    }

    @Nested
    @SpringBootTest
    inner class DefaultProfileTests : MockSpringVaadinTests() {
        @Test
        fun `should not open the showcase without the dev profile`() {
            assertThrows<NotFoundError> { UI.getCurrent().navigate("dev/showcase") }
        }
    }
}
