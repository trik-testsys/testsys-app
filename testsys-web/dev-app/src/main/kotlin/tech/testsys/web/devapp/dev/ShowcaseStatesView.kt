package tech.testsys.web.devapp.dev

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.signals.Signal
import com.vaadin.flow.signals.local.ValueSignal
import org.springframework.core.env.Environment
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.layout.SlotRowScope
import tech.testsys.web.components.navigation.TabsScope
import tech.testsys.web.components.navigation.pagination
import tech.testsys.web.components.navigation.pills
import tech.testsys.web.components.navigation.tabs
import tech.testsys.web.components.overlay.menu
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

private const val ROW_COUNT = 36
private const val PAGE_SIZE = 8
private const val ACCEPTED_EVERY = 3
private const val HALF = 12
private const val LIVE_ROW_COUNT = 5
private const val LIVE_SUBMISSION_ID_START = 900_000L
private const val LIVE_SUBMISSION_SCORE = 87
private const val PAGINATION_PAGES = 20
private const val PAGINATION_ROWS = 3
private val LOAD_DELAY: Duration = Duration.ofSeconds(1)
private val NEW_ROW_DELAY: Duration = Duration.ofSeconds(2)
private val CLOCK_PERIOD: Duration = Duration.ofSeconds(1)
private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
private val LOAD_SAMPLE_ROWS = listOf(
    "Тур «Весенний кубок» опубликован",
    "Участнику начислено 82 балла",
    "Открыта регистрация на «Летний марафон»",
)
private val liveSubmissionId: AtomicLong = AtomicLong(LIVE_SUBMISSION_ID_START)

/** Filter of the showcase rows by their verdict. */
private enum class RowFilter(val label: String) {
    All("Все посылки"),
    Accepted("Принятые"),
    Failed("С ошибками"),
}

/** Category of the pill showcase. */
private enum class Category(val label: String) {
    All("Все"),
    Olympiads("Олимпиады"),
    Contests("Контесты"),
    Quizzes("Квизы"),
}

/** Row of the showcase tables. */
private class StateRow(val id: Int, val author: String) {
    val isAccepted: Boolean
        get() = id % ACCEPTED_EVERY == 0
}

private val ROWS: List<StateRow> = (1..ROW_COUNT).map { id -> StateRow(id, "Участник $id") }

/**
 * Second showcase page: page head, block tabs and pills, a pagination, empty states, the load failure of a table,
 * action menus and live updates (a clock bound to a signal, background block loads and table refreshes); it opens only in the `dev`
 * profile.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/states")
@PageTitle("Витрина: навигация и состояния")
class ShowcaseStatesView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    private val clock = ValueSignal(currentClockText())
    private var clockExecutor: ScheduledExecutorService? = null

    init {
        page(showcaseHeader()) {
            showcaseHead("Навигация и состояния")
            block(title = "Доступность") {
                actions {
                    action("Открыть примеры доступности") { onClick { UI.getCurrent().navigate(ShowcaseNestedView::class.java) } }
                }
            }
            tabsSection()
            pillsSection()
            paginationSection()
            emptySection()
            failureSection()
            menuSection()
            liveSection(clock)
        }
        addAttachListener { startClock() }
        addDetachListener { stopClock() }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }

    /** Starts the clock executor, unless it is already running: an attach without a detach in between is a no-op. */
    private fun startClock() {
        if (clockExecutor != null) return
        val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "showcase-clock").apply { isDaemon = true }
        }
        val periodMillis = CLOCK_PERIOD.toMillis()
        executor.scheduleAtFixedRate({ clock.set(currentClockText()) }, periodMillis, periodMillis, TimeUnit.MILLISECONDS)
        clockExecutor = executor
    }

    /** Stops the clock executor, unless it is not running: a detach without a matching attach is a no-op. */
    private fun stopClock() {
        clockExecutor?.shutdownNow()
        clockExecutor = null
    }
}

/** The current time as `HH:mm:ss`. */
private fun currentClockText(): String = LocalTime.now().format(CLOCK_FORMAT)

/** Blocks the current thread for [duration]; simulates the latency of a real fetch in showcase loads. */
private fun pause(duration: Duration) {
    try {
        Thread.sleep(duration.toMillis())
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}

private fun PageScope.tabsSection() {
    row {
        slot(size = HALF) {
            row {
                block {
                    val filter = tabs(initial = RowFilter.All) { filterTabs() }
                    actions { badge("Вкладки вместо заголовка", Tone.Info) }
                    val rows = table(
                        key = { row -> row.id },
                        pageSize = PAGE_SIZE,
                        fetch = { request -> filtered(filter.value, request) },
                    ) {
                        codeColumn("ID") { row -> row.id.toString() }
                        textColumn("Автор") { row -> row.author }
                    }
                    filter.onChange { rows.refresh(toFirstPage = true) }
                }
            }
        }
        slot(size = HALF) {
            row {
                block(title = "Посылки тура", subtitle = "Заголовок и вкладки второй строкой") {
                    val filter = tabs(initial = RowFilter.All) { filterTabs() }
                    actions {
                        action("Экспорт", IconName.Download) {
                            onClick { toast(FeedbackKind.Info, "Экспорт демонстрационной таблицы") }
                        }
                    }
                    val rows = table(
                        key = { row -> row.id },
                        pageSize = PAGE_SIZE,
                        fetch = { request -> filtered(filter.value, request) },
                    ) {
                        codeColumn("ID") { row -> row.id.toString() }
                        textColumn("Автор") { row -> row.author }
                    }
                    filter.onChange { rows.refresh(toFirstPage = true) }
                    footer {
                        linkAction("Обнулить счётчик ошибок") { onClick { filter.setCount(RowFilter.Failed, null) } }
                    }
                }
            }
        }
    }
}

private fun TabsScope<RowFilter>.filterTabs() {
    tab(RowFilter.All, RowFilter.All.label)
    tab(RowFilter.Accepted, RowFilter.Accepted.label, count = ROWS.count { row -> row.isAccepted })
    tab(RowFilter.Failed, RowFilter.Failed.label, count = ROWS.count { row -> !row.isAccepted }, countKind = CounterKind.Attention)
}

private fun filtered(filter: RowFilter, request: PageRequest): Page<StateRow> {
    val rows = when (filter) {
        RowFilter.All -> ROWS
        RowFilter.Accepted -> ROWS.filter { row -> row.isAccepted }
        RowFilter.Failed -> ROWS.filterNot { row -> row.isAccepted }
    }
    return Page(rows.drop(request.offset).take(request.limit), rows.size)
}

private fun PageScope.pillsSection() {
    block(title = "Пилюли", subtitle = "В шапке блока и в строке блока") {
        lateinit var chosen: TextHandle
        actions {
            val category = pills(initial = Category.All) {
                Category.entries.forEach { value -> pill(value, value.label) }
            }
            category.onChange { value -> chosen.text = "Выбрано в шапке: ${value.label}" }
        }
        row { chosen = text("Выбрано в шапке: ${Category.All.label}") }
        row {
            pills(initial = Category.Contests, size = HALF) {
                Category.entries.forEach { value -> pill(value, value.label) }
            }.onChange { value -> chosen.text = "Выбрано в строке: ${value.label}" }
        }
    }
}

/** A pagination of many pages bound to a [ValueSignal] of the page, which the rows below follow. */
private fun PageScope.paginationSection() {
    val page = ValueSignal(1)
    block(title = "Пагинация", subtitle = "Длинный диапазон — с пропусками; страница хранится в сигнале") {
        row {
            horizontal {
                pagination(pageCount = PAGINATION_PAGES) {
                    bindPage(page)
                    onChange { chosen -> page.set(chosen) }
                }
            }
        }
        row { text(page.map { current -> "Страница $current из $PAGINATION_PAGES" }) }
        for (line in 1..PAGINATION_ROWS) {
            row { text(page.map { current -> "Участник ${(current - 1) * PAGINATION_ROWS + line}" }) }
        }
    }
}

private fun PageScope.emptySection() {
    row {
        slot(size = HALF) {
            row {
                block(title = "Пустой блок") {
                    emptyState("Посылок пока нет", description = "Отправьте решение любой задачи.", icon = IconName.Upload) {
                        action("К задачам") { onClick { toast(FeedbackKind.Info, "Переход к задачам") } }
                    }
                }
            }
        }
        slot(size = HALF) {
            row {
                block(title = "Пустая таблица") {
                    table<StateRow>(key = { row -> row.id }, fetch = { Page(emptyList(), 0) }) {
                        textColumn("Автор") { row -> row.author }
                        empty("Ничего не найдено", description = "Измените условия поиска.", icon = IconName.Search) {
                            action("Сбросить фильтр") { onClick { toast(FeedbackKind.Info, "Фильтр сброшен") } }
                        }
                    }
                }
            }
        }
    }
}

private fun PageScope.failureSection() {
    block(title = "Ошибка загрузки", subtitle = "Первая загрузка падает, «Повторить» загружает строки") {
        var failures = 1
        val rows = table(
            key = { row -> row.id },
            pageSize = PAGE_SIZE,
            fetch = { request ->
                if (failures > 0) {
                    failures--
                    error("Showcase fetch failure")
                }
                filtered(RowFilter.All, request)
            },
        ) {
            codeColumn("ID") { row -> row.id.toString() }
            textColumn("Автор") { row -> row.author }
        }
        footer {
            linkAction("Сломать снова") {
                onClick {
                    failures = 1
                    rows.refresh()
                }
            }
        }
    }
}

private fun PageScope.menuSection() {
    row {
        slot(size = HALF) {
            row {
                block(title = "Меню в шапке") {
                    actions {
                        menu(label = "Ещё") {
                            item("Экспорт") { toast(FeedbackKind.Info, "Экспорт") }
                            item("Недоступно", isEnabled = false) {}
                        }
                        menu {
                            item("Открыть") { toast(FeedbackKind.Info, "Открыть") }
                            item("Дублировать") { toast(FeedbackKind.Info, "Дублировать") }
                            destructiveItem("Удалить") { toast(FeedbackKind.Error, "Удалить") }
                        }
                    }
                    row { text("Разрушительные пункты — последними, после разделителя") }
                }
            }
        }
        slot(size = HALF) {
            row {
                block(title = "Меню в строках") {
                    table(key = { row -> row.id }, pageSize = PAGE_SIZE, fetch = { request -> filtered(RowFilter.All, request) }) {
                        codeColumn("ID") { row -> row.id.toString() }
                        textColumn("Автор") { row -> row.author }
                        onRowClick { row -> toast(FeedbackKind.Info, "Строка ${row.id}") }
                        menuColumn(ariaLabel = { row -> "Действия с посылкой №${row.id}" }) { row ->
                            item("Открыть") { toast(FeedbackKind.Info, "Открыть ${row.id}") }
                            item("Перепроверить") { toast(FeedbackKind.Info, "Перепроверить ${row.id}") }
                            destructiveItem("Дисквалифицировать") { toast(FeedbackKind.Error, "Дисквалифицировать ${row.id}") }
                        }
                    }
                }
            }
        }
    }
}

/** Showcase of `load()`, a table refreshed from the background and a clock following a [Signal]. */
private fun PageScope.liveSection(clock: Signal<String>) {
    row {
        slot(size = HALF) { row { loadBlock() } }
        slot(size = HALF) { row { loadFailureBlock() } }
    }
    row {
        slot(size = HALF) { row { liveSubmissionsBlock() } }
        slot(size = HALF) { row { clockBlock(clock) } }
    }
}

private fun SlotRowScope.loadBlock() {
    block(title = "Загрузка", subtitle = "Скелетон, пока фоновый запрос выполняется") {
        val handle = load({
            pause(LOAD_DELAY)
            LOAD_SAMPLE_ROWS
        }) { rows -> rows.forEach { line -> row { text(line) } } }
        actions { action("Загрузить снова") { onClick { handle.reload() } } }
    }
}

private fun SlotRowScope.loadFailureBlock() {
    val attempts = AtomicInteger()
    block(title = "Ошибка загрузки", subtitle = "Первая загрузка падает, «Повторить» показывает успех") {
        load({
            pause(LOAD_DELAY)
            if (attempts.getAndIncrement() == 0) error("Showcase load failure")
            LOAD_SAMPLE_ROWS
        }) { rows -> rows.forEach { line -> row { text(line) } } }
    }
}

private fun SlotRowScope.liveSubmissionsBlock() {
    val submissions: MutableList<ShowcaseSubmission> = CopyOnWriteArrayList(SUBMISSIONS.take(LIVE_ROW_COUNT))
    block(title = "Посылки", subtitle = "«Новая посылка» добавляет строку в фоне; новая строка подсвечивается") {
        val rows = table(
            key = { row -> row.id },
            fetch = { request -> Page(submissions.drop(request.offset).take(request.limit), submissions.size) },
        ) { submissionColumns() }
        actions {
            action("Новая посылка через 2 с") {
                onClick {
                    thread(isDaemon = true, name = "showcase-new-submission") {
                        pause(NEW_ROW_DELAY)
                        submissions.add(0, nextLiveSubmission())
                        rows.refresh()
                    }
                }
            }
        }
    }
}

private fun SlotRowScope.clockBlock(clock: Signal<String>) {
    block(title = "Часы", subtitle = "Обновляются раз в секунду через ValueSignal") {
        row { text(clock) }
    }
}

/** A freshly "arrived" submission for the live table, with a unique id of its own range. */
private fun nextLiveSubmission(): ShowcaseSubmission = ShowcaseSubmission(
    id = liveSubmissionId.incrementAndGet(),
    author = "Новый участник",
    task = "E. Новая задача",
    verdict = ShowcaseVerdict.Accepted,
    score = LIVE_SUBMISSION_SCORE,
    sentAt = LocalDateTime.now(),
)
