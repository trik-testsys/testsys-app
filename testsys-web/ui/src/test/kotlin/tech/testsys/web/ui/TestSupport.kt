package tech.testsys.web.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.Route
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.layout.renderPage
import tech.testsys.web.ui.navigation.CabinetHeader
import java.time.DayOfWeek
import java.util.Locale

/** Texts of the tests; values are distinct so that assertions can tell them apart. */
internal val testTexts = UiTexts(
    locale = Locale.forLanguageTag("ru-RU"),
    brand = "TestSys",
    signIn = "Войти",
    calendar = CalendarTexts(
        monthNames = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        ),
        weekdays = listOf("воскресенье", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота"),
        weekdaysShort = listOf("вс", "пн", "вт", "ср", "чт", "пт", "сб"),
        firstDayOfWeek = DayOfWeek.MONDAY,
        dateFormat = "dd.MM.yyyy",
        today = "Сегодня",
        cancel = "Отмена",
    ),
    fieldErrors = FieldErrorTexts(
        badInput = "Проверьте формат",
        belowMin = "Меньше допустимого",
        aboveMax = "Больше допустимого",
        stepMismatch = "Не по шагу",
    ),
    dateRangeReversed = "Конец раньше начала",
    editing = EditingTexts(start = "Изменить", save = "Сохранить", cancel = "Отменить"),
    table = TableTexts(
        empty = "Пусто",
        range = { from, to, total -> "$from–$to из $total" },
        previous = "Назад",
        next = "Вперёд",
        selectAll = "Отметить всё на странице",
        selectRow = "Отметить строку",
    ),
    dialog = DialogTexts(cancel = "Отказаться", close = "Закрыть", typeToConfirm = { name -> "Введите $name" }),
    lookup = LookupTexts(search = "Поиск", open = "Выбрать", clear = "Очистить", empty = "Не найдено"),
)

/** Starts a mocked Vaadin UI with the test routes of this package before each test. */
abstract class MockVaadinTests {
    @BeforeEach
    fun setUpVaadin() = MockVaadin.setup(Routes().autoDiscoverViews("tech.testsys.web.ui"))

    @AfterEach
    fun tearDownVaadin() = MockVaadin.tearDown()
}

/** Route target of navigation tests. */
@Route("test/first")
class FirstTestView : Div()

/** Second route target of navigation tests. */
@Route("test/second")
class SecondTestView : Div()

internal fun Component.child(index: Int): Component = children.toList()[index]

internal fun Component.classes(): Set<String> = element.classList

/** All components of the subtree, this one included, that carry [cssClass]. */
internal fun Component.findAll(cssClass: String): List<Component> =
    (listOf(this) + descendants()).filter { component -> cssClass in component.element.classList }

internal fun Component.find(cssClass: String): Component = findAll(cssClass).single()

private fun Component.descendants(): List<Component> = children.toList().flatMap { child -> listOf(child) + child.descendants() }

/** Builds a page with an empty header, attaches it to the current UI and returns its `main.ts-page`. */
internal fun buildTestPage(body: PageScope.() -> Unit): Component {
    val root = Div()
    renderPage(root, CabinetHeader(), testTexts, body)
    UI.getCurrent().add(root)
    return root.child(1)
}

/** Builds a full-width block with one row filled by [content] and returns the row. */
internal fun buildTestRow(content: BlockRowScope.() -> Unit): Component = buildTestPage { block { row(content) } }.find("ts-block__row")

/** Builds a full-width block whose only row holds a vertical group filled by [content] and returns the group. */
internal fun buildTestContent(content: ContentScope.() -> Unit): Component = buildTestRow { vertical { content() } }.child(0)

/** The Vaadin control of the grid field labelled [label] on the current UI. */
internal inline fun <reified C : Component> control(label: String): C {
    val caption = UI.getCurrent().findAll("ts-field__text").single { text -> text.element.text == label }
    val field = caption.parent.orElseThrow().parent.orElseThrow()
    return field.find("ts-field__value").child(0) as C
}

/** The button with [text] on the current UI, shown or hidden. */
internal fun button(text: String): Button {
    val ui = UI.getCurrent()
    return (listOf<Component>(ui) + ui.descendants()).filterIsInstance<Button>().single { button -> button.text == text }
}

/** Dialogs opened on the current UI after the pending round trip attached them. */
internal fun openDialogs(): List<Dialog> {
    MockVaadin.clientRoundtrip()
    return _find<Dialog>().filter { dialog -> dialog.isOpened }
}
