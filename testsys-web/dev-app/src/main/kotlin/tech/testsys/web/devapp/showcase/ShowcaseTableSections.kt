package tech.testsys.web.devapp.showcase

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.filterChip

private const val SMALL_TABLE_ROWS = 3

private const val SCORE_SORT = "score"

private const val TIME_SORT = "time"

/**
 * Paged table of submissions with column widths, head filters by verdict and by errors, row selection with a recheck of
 * the selected rows; an empty table and a table of one page.
 */
internal fun PageScope.tableSection() {
    tableFiltersExample()

    var verdict: ShowcaseVerdict? = null
    var isErrorsOnly = false
    row {
        block(title = "Посылки", subtitle = "Ширины колонок, сортировка по баллам и времени, фильтры в шапке, выбор строк") {
            val submissions = table(
                key = { row -> row.id },
                selectable = true,
                fetch = { request -> submissionPage(SUBMISSIONS.filter { row -> row.matches(verdict, isErrorsOnly) }, request) },
            ) {
                submissionColumns()
                onRowClick { row -> toast(FeedbackKind.Info, "Посылка #${row.id}") }
            }
            actions {
                select("Вердикт", items = ShowcaseVerdict.entries, itemLabel = ShowcaseVerdict::label, emptyLabel = "Все вердикты") {
                    addValueChangeListener { event ->
                        verdict = event.value
                        submissions.refresh(toFirstPage = true)
                    }
                }
                filterChip("С ошибками") {
                    onChange { isOn ->
                        isErrorsOnly = isOn
                        submissions.refresh(toFirstPage = true)
                    }
                }
                text(submissions.selection.map { keys -> "Выбрано: ${keys.size}" })
                action("Перепроверить") {
                    bindEnabled(submissions.selection.map { keys -> keys.isNotEmpty() })
                    onClick {
                        val count = submissions.selected.size
                        toast(kind = FeedbackKind.Success, title = "Отправлено на перепроверку", description = "Посылок: $count")
                        submissions.clearSelection()
                    }
                }
            }
        }
    }

    row {
        block(size = 12, title = "Пустая таблица") {
            table(key = { row -> row.id }, fetch = { request -> submissionPage(emptyList(), request) }) {
                submissionColumns(isCompact = true)
                empty("Посылок пока нет")
            }
        }
        block(size = 12, title = "Маленькая таблица", subtitle = "Одна страница — без пагинации") {
            table(key = { row -> row.id }, fetch = { request -> submissionPage(SUBMISSIONS.take(SMALL_TABLE_ROWS), request) }) {
                codeColumn("ID", size = 4) { row -> "#${row.id}" }
                textColumn("Участник", size = 12) { row -> row.author }
                column("Вердикт", size = 8) { row -> badge(row.verdict.label, row.verdict.tone) }
            }
        }
    }
}

/** Whether the submission passes the head filters: [verdict] if one is chosen, and a failed verdict if [isErrorsOnly]. */
private fun ShowcaseSubmission.matches(verdict: ShowcaseVerdict?, isErrorsOnly: Boolean): Boolean =
    (verdict == null || this.verdict == verdict) && (!isErrorsOnly || this.verdict.isError)

/** The page of [rows] asked by [request], sorted by the showcase sort keys. */
internal fun submissionPage(rows: List<ShowcaseSubmission>, request: PageRequest): Page<ShowcaseSubmission> {
    val sorted = when (request.sort?.key) {
        SCORE_SORT -> rows.sortedBy { row -> row.score }
        TIME_SORT -> rows.sortedBy { row -> row.sentAt }
        else -> rows
    }

    val ordered = if (request.sort?.isDescending == true) sorted.reversed() else sorted
    return Page(ordered.drop(request.offset).take(request.limit), ordered.size)
}
