package tech.testsys.web.devapp.showcase

import com.vaadin.flow.signals.local.ValueSignal
import tech.testsys.web.components.TextHandle
import tech.testsys.web.components.display.text
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.pagination
import tech.testsys.web.components.navigation.pills

private const val HALF = 12

private const val PAGINATION_PAGES = 20

private const val PAGINATION_ROWS = 3

/** Category of the pill showcase. */
private enum class Category(val label: String) {
    All("Все"),
    Olympiads("Олимпиады"),
    Contests("Контесты"),
    Quizzes("Квизы"),
}

internal fun PageScope.pillsSection() {
    row {
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
}

/** A pagination of many pages bound to a [ValueSignal] of the page, which the rows below follow. */
internal fun PageScope.paginationSection() {
    val page = ValueSignal(1)
    row {
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
}
