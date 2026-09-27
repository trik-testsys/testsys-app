package tech.testsys.web.dev

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.ui.TestSysView
import tech.testsys.web.ui.TextHandle
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.actions.linkAction
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.data.Page
import tech.testsys.web.ui.data.PageRequest
import tech.testsys.web.ui.data.table
import tech.testsys.web.ui.display.CounterKind
import tech.testsys.web.ui.display.Tone
import tech.testsys.web.ui.display.badge
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.feedback.FeedbackKind
import tech.testsys.web.ui.feedback.emptyState
import tech.testsys.web.ui.feedback.toast
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.navigation.TabsScope
import tech.testsys.web.ui.navigation.pills
import tech.testsys.web.ui.navigation.tabs
import tech.testsys.web.ui.overlay.menu

private const val ROW_COUNT = 36
private const val PAGE_SIZE = 8
private const val ACCEPTED_EVERY = 3
private const val HALF = 12

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
 * Second showcase page: page head, block tabs and pills, empty states, the load failure of a table and action menus;
 * it opens only in the `dev` profile.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/states")
@PageTitle("Витрина: навигация и состояния")
class ShowcaseStatesView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Навигация и состояния")
            tabsSection()
            pillsSection()
            emptySection()
            failureSection()
            menuSection()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
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
                    actions { action("Экспорт", IconName.Download) }
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
        row { pills(initial = Category.Contests, size = HALF) { Category.entries.forEach { value -> pill(value, value.label) } } }
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
                        menuColumn { row ->
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
