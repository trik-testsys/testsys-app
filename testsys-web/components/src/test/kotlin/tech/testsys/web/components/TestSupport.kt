package tech.testsys.web.components

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
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.layout.renderPage
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.texts.CalendarTexts
import tech.testsys.web.components.texts.ComponentTexts
import tech.testsys.web.components.texts.DateFieldTexts
import tech.testsys.web.components.texts.DialogTexts
import tech.testsys.web.components.texts.EditingTexts
import tech.testsys.web.components.texts.FieldErrorTexts
import tech.testsys.web.components.texts.FooterTexts
import tech.testsys.web.components.texts.HeaderTexts
import tech.testsys.web.components.texts.LoadTexts
import tech.testsys.web.components.texts.LookupTexts
import tech.testsys.web.components.texts.MenuTexts
import tech.testsys.web.components.texts.NavigationTexts
import tech.testsys.web.components.texts.NotFoundTexts
import tech.testsys.web.components.texts.PaginationTexts
import tech.testsys.web.components.texts.TableFiltersTexts
import tech.testsys.web.components.texts.TableTexts
import tech.testsys.web.components.texts.UiTexts
import java.time.DayOfWeek
import java.util.Locale

/** Texts of the tests; values are distinct so that assertions can tell them apart. */
internal val testTexts = UiTexts(
    locale = Locale.forLanguageTag("ru-RU"),
    brand = "TestSys",
    signIn = "Войти",
    footer = FooterTexts(year = { year -> year.toString() }, links = "Ссылки подвала"),
    tableFilters = TableFiltersTexts(title = "Фильтры", apply = "Применить", reset = "Сбросить"),
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
        selectAll = "Отметить всё на странице",
        selectRow = "Отметить строку",
    ),
    pagination = PaginationTexts(previous = "Назад", next = "Вперёд", page = { page -> "Стр. $page" }),
    load = LoadTexts(failed = "Сбой загрузки", failedHint = "Повторите позже", retry = "Ещё раз"),
    dialog = DialogTexts(cancel = "Отказаться", close = "Закрыть", typeToConfirm = { name -> "Введите $name" }),
    lookup = LookupTexts(
        search = "Поиск",
        open = "Выбрать",
        clear = "Очистить",
        empty = "Не найдено",
        remove = { value -> "Убрать $value" },
        reset = "Снять все",
        apply = "Применить",
        selectedCount = { count -> "Отмечено: $count" },
    ),
    navigation = NavigationTexts(breadcrumbs = "Цепочка", sections = "Разделы страницы"),
    menu = MenuTexts(actions = "Меню действий"),
    dateFields = DateFieldTexts(
        date = "дата",
        time = "время",
        rangeFrom = { label -> "$label: с" },
        rangeTo = { label -> "$label: до" },
        rangeFromPrefix = "С",
        rangeToPrefix = "До",
        rangeRequired = "Укажите хотя бы одну границу периода",
    ),
    components = ComponentTexts(
        selectAll = "Выбрать все",
        previousMonth = "Предыдущий месяц",
        nextMonth = "Следующий месяц",
        calendar = "Открыть календарь",
        drag = "Переместить: пробел, затем стрелки; Enter применяет, Escape отменяет",
        upload = "Выбрать файлы",
        drop = "Перетащите файлы сюда",
        cancel = "Отменить",
        preparing = "Подготовка",
        downloading = "Передача",
        done = "Передано сервером",
        failed = "Ошибка передачи",
        retry = "Повторить",
        downloadAgain = "Скачать снова",
        uploadRejected = "Файл не соответствует ограничениям",
        loading = "Загрузка",
        overflow = "Другие участники",
        openCalendar = { label -> "Календарь: $label" },
        uploadLimits = { count, bytes -> "$count по $bytes байт" },
        difficultyLabels = listOf("Лёгкая", "Средняя", "Сложная"),
        questionStatus = { number, answered, flagged -> "$number: $answered, $flagged" },
        reorderPosition = { label, position, total -> "$label: $position/$total" },
        downloadLabel = { label, state -> "$label: $state" },
        percent = { value -> "$value%" },
        byteUnits = listOf("Б", "КБ", "МБ", "ГБ"),
        timerUnits = listOf("дни", "часы", "минуты", "секунды"),
        avatarOverflow = { count -> "Ещё $count" },
        transferBytes = { count -> "Байты: $count" },
        question = { number -> "Вопрос $number" },
    ),
    header = HeaderTexts(
        search = "Поиск",
        searchLoading = "Ищем",
        searchEmpty = "Пустой поиск",
        searchFailed = "Ошибка поиска",
        searchCount = { count -> "Результатов: $count" },
        retry = "Повтор поиска",
        notifications = "Уведомления",
        notificationsEmpty = "Пустые уведомления",
        readAll = "Прочитать всё",
        unreadCount = { count -> "Непрочитанных: $count" },
        unreadItem = { label -> "Не прочитано: $label" },
        userMenu = { name -> "Меню: $name" },
        arrivalOpen = "Открыть поступление",
        arrivalClose = "Закрыть поступление",
        arrivalCount = { count -> "Получено: $count" },
        arrivalBatchHint = "Открыть список",
    ),
    notFound = NotFoundTexts(
        title = "Страница не найдена",
        description = "Проверьте адрес",
        back = "Назад",
        pageTitle = "Страница не найдена — TestSys",
    ),
)

/** Starts a mocked Vaadin UI with the test routes of this package before each test. */
abstract class MockVaadinTests {
    @BeforeEach
    fun setUpVaadin() = MockVaadin.setup(Routes().autoDiscoverViews("tech.testsys.web.components"))

    @AfterEach
    fun tearDownVaadin() = MockVaadin.tearDown()
}

/** Route target of navigation tests. */
@Route("test/first")
class FirstTestView : Div()

/** Second route target of navigation tests. */
@Route("test/second")
class SecondTestView : Div()

/** Route target with a parameter for page tab tests. */
@Route("test/item/:id")
class ItemTestView : Div()

internal fun Component.child(index: Int): Component = children.toList()[index]

internal fun Component.classes(): Set<String> = element.classList

/** All components of the subtree, this one included, that carry [cssClass]. */
internal fun Component.findAll(cssClass: String): List<Component> =
    (listOf(this) + descendants()).filter { component -> cssClass in component.element.classList }

internal fun Component.find(cssClass: String): Component = findAll(cssClass).single()

private fun Component.descendants(): List<Component> = children.toList().flatMap { child -> listOf(child) + child.descendants() }

/** Builds a page of [view] with an empty header, attaches it to the current UI and returns its `main.ts-page`. */
internal fun buildTestPage(view: Class<out Component>? = null, body: PageScope.() -> Unit): Component {
    val root = Div()
    renderPage(root, CabinetHeader(), testTexts, view, body)
    UI.getCurrent().add(root)
    return root.children.toList().single { child -> child.element.tag == "main" }
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

/** All buttons of the subtree of [root], shown or hidden. */
internal fun findAllButtons(root: Component): List<Button> = (listOf(root) + root.descendants()).filterIsInstance<Button>()

/** The button with [text] on the current UI, shown or hidden. */
internal fun button(text: String): Button = findAllButtons(UI.getCurrent()).single { button -> button.text == text }

/** Dialogs opened on the current UI after the pending round trip attached them. */
internal fun openDialogs(): List<Dialog> {
    MockVaadin.clientRoundtrip()
    return _find<Dialog>().filter { dialog -> dialog.isOpened }
}
