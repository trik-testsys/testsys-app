package tech.testsys.web.devapp.showcase

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.ProgressValue
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.progressBar
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.TabsScope
import tech.testsys.web.components.navigation.pills
import tech.testsys.web.components.navigation.tabs
import tech.testsys.web.components.overlay.menu

private const val ROW_COUNT = 36

private const val PAGE_SIZE = 8

private const val ACCEPTED_EVERY = 3

private const val HALF = 12

private object HighlightTableSamples {
    const val COLUMN_SIZE = 6
    const val PROGRESS_PERCENT = 50.0
    val ROWS = listOf("Первая строка", "Вторая строка")
}

/** Filter of the showcase rows by their verdict. */
private enum class RowFilter(val label: String) {
    All("Все посылки"),
    Accepted("Принятые"),
    Failed("С ошибками"),
}

/** Row of the showcase tables. */
private class StateRow(val id: Int, val author: String) {
    val isAccepted: Boolean
        get() = id % ACCEPTED_EVERY == 0
}

private val ROWS: List<StateRow> = (1..ROW_COUNT).map { id -> StateRow(id, "Участник $id") }

/** Shows the shared table surface and readonly controls inside an inverse block. */
internal fun PageScope.highlightedTableSection() {
    highlightBlock(title = "Таблица в выделенном блоке") {
        table(key = { row: String -> row }, fetch = { Page(rows = HighlightTableSamples.ROWS, total = HighlightTableSamples.ROWS.size) }) {
            textColumn("Значение", size = HighlightTableSamples.COLUMN_SIZE) { row -> row }
            column("Поле только для чтения", size = HighlightTableSamples.COLUMN_SIZE) { row ->
                select("Список: $row", items = listOf(row), itemLabel = { item -> item }) {
                    value = row
                    isEditable = false
                }
            }
            column("Ход", size = HighlightTableSamples.COLUMN_SIZE) { row ->
                progressBar(label = "Ход: $row", value = ProgressValue.Determinate(percent = HighlightTableSamples.PROGRESS_PERCENT))
            }
            column("Варианты", size = HighlightTableSamples.COLUMN_SIZE) {
                pills(initial = "first") {
                    pill(value = "first", label = "Первый")
                    pill(value = "second", label = "Второй")
                }
            }
        }
    }
}

internal fun PageScope.tabsSection() {
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
                        codeColumn("ID", size = 6) { row -> row.id.toString() }
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
                        action("Экспортировать", IconName.Download) {
                            onClick { toast(FeedbackKind.Info, "Экспорт демонстрационной таблицы") }
                        }
                    }
                    val rows = table(
                        key = { row -> row.id },
                        pageSize = PAGE_SIZE,
                        fetch = { request -> filtered(filter.value, request) },
                    ) {
                        codeColumn("ID", size = 6) { row -> row.id.toString() }
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

internal fun PageScope.emptySection() {
    row {
        slot(size = HALF) {
            row {
                block(title = "Пустой блок") {
                    emptyState("Посылок пока нет", description = "Отправьте решение любой задачи.", icon = IconName.Upload) {
                        action("Перейти к задачам") { onClick { toast(FeedbackKind.Info, "Переход к задачам") } }
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

internal fun PageScope.failureSection() {
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
            codeColumn("ID", size = 3) { row -> row.id.toString() }
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

internal fun PageScope.menuSection() {
    row {
        slot(size = HALF) {
            row {
                block(title = "Меню в шапке") {
                    actions {
                        menu(label = "Ещё") {
                            item("Экспортировать") { toast(FeedbackKind.Info, "Экспорт") }
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
                        codeColumn("ID", size = 6) { row -> row.id.toString() }
                        textColumn("Автор", size = 10) { row -> row.author }
                        column("Выбор", size = 6) { select("Статус", listOf("Новый", "Принят"), { label -> label }) }
                        onRowClick { row -> toast(FeedbackKind.Info, "Строка ${row.id}") }
                        menuColumn(size = 2, ariaLabel = { row -> "Действия с посылкой №${row.id}" }) { row ->
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

internal fun PageScope.wideMatrixSection() {
    val rows = (1..WideMatrix.ROW_COUNT).toList()
    block(title = "Широкая матрица результатов", subtitle = "Все результатные столбцы сохраняются; прокрутка остаётся внутри блока") {
        table(
            key = { id: Int -> id },
            pageSize = WideMatrix.PAGE_SIZE,
            gridColumns = WideMatrix.IDENTITY_SIZE + WideMatrix.TASK_COUNT * WideMatrix.RESULT_SIZE,
            fetch = { request -> Page(rows.drop(request.offset).take(request.limit), rows.size) },
        ) {
            textColumn("Участник", size = WideMatrix.IDENTITY_SIZE) { id -> "Группа $id" }
            repeat(WideMatrix.TASK_COUNT) { index ->
                numberColumn("Задача ${index + 1}", size = WideMatrix.RESULT_SIZE) { id -> id * (index + 1) }
            }
        }
    }
}

private object WideMatrix {
    const val ROW_COUNT = 2
    const val TASK_COUNT = 30
    const val IDENTITY_SIZE = 6
    const val RESULT_SIZE = 2
    const val PAGE_SIZE = 1
}
