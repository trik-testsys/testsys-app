package tech.testsys.web.devapp.showcase

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.signals.local.ValueSignal
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.checkbox
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope

private const val FILTER_PAGE_SIZE = 5

private const val DEFAULT_FILTER_SCORE = 20

/** Criteria of the form-filter example; the table only reads a published immutable snapshot. */
private data class SubmissionFilters(
    val author: String = "",
    val verdict: ShowcaseVerdict? = null,
    val minScore: Int? = DEFAULT_FILTER_SCORE,
    val period: DateRange = DateRange(),
    val isErrorsOnly: Boolean = false,
)

/** Select options of the example; All maps to an unrestricted verdict criterion. */
private enum class SubmissionVerdictChoice(val verdict: ShowcaseVerdict?) {
    All(null),
    Accepted(ShowcaseVerdict.Accepted),
    WrongAnswer(ShowcaseVerdict.WrongAnswer),
    TimeLimit(ShowcaseVerdict.TimeLimit),
    Running(ShowcaseVerdict.Running),
    ;

    val label: String
        get() = verdict?.label ?: "Все вердикты"
}

/** Buffered Binder target; a successful write builds the next applied snapshot. */
private class SubmissionFiltersBuilder(source: SubmissionFilters) {
    var author: String = source.author
    var verdict: ShowcaseVerdict? = source.verdict
    var minScore: Int? = source.minScore
    var period: DateRange = source.period
    var isErrorsOnly: Boolean = source.isErrorsOnly

    fun build(): SubmissionFilters = SubmissionFilters(
        author = author,
        verdict = verdict,
        minScore = minScore,
        period = period,
        isErrorsOnly = isErrorsOnly,
    )
}

internal fun PageScope.tableFiltersExample() {
    val defaults = SubmissionFilters()
    val applied = ValueSignal(defaults)
    val binder = Binder<SubmissionFiltersBuilder>()
    row {
        block(title = "Посылки: фильтры полями", subtitle = "До применения таблица не меняется; минимум по умолчанию — 20 баллов") {
            val rows = table(
                key = { row: ShowcaseSubmission -> row.id },
                pageSize = FILTER_PAGE_SIZE,
                selectable = true,
                fetch = { request -> submissionPage(SUBMISSIONS.filter { row -> row.matches(applied.peek()) }, request) },
            ) { submissionColumns() }
            actions {
                action("Обновить выборку") { onClick { rows.refresh() } }
                text(
                    applied.map { criteria ->
                        "Применено: минимум ${criteria.minScore ?: "любой"}, участник ${criteria.author.ifEmpty { "любой" }}"
                    },
                )
            }
            filters(
                onApply = {
                    val candidate = SubmissionFiltersBuilder(defaults)
                    val isValid = binder.writeBeanIfValid(candidate)
                    if (isValid) applied.set(candidate.build())
                    isValid
                },
                onReset = {
                    applied.set(defaults)
                    binder.readBean(SubmissionFiltersBuilder(defaults))
                },
                onRefresh = { rows.refresh(toFirstPage = true) },
            ) {
                row {
                    textInput("Участник", labelSize = 4, size = 8, hint = "Часть имени, без учёта регистра") {
                        binder.forField(this).bind({ draft -> draft.author }, { draft, value -> draft.author = value })
                    }
                    select(
                        "Вердикт",
                        labelSize = 4,
                        size = 8,
                        items = SubmissionVerdictChoice.entries,
                        itemLabel = SubmissionVerdictChoice::label,
                    ) {
                        binder.forField(this).bind(
                            { draft -> SubmissionVerdictChoice.entries.first { choice -> choice.verdict == draft.verdict } },
                            { draft, value -> draft.verdict = value?.verdict },
                        )
                    }
                }
                row {
                    integerInput(
                        "Минимум баллов",
                        labelSize = 4,
                        size = 8,
                        min = 0,
                        max = SCORE_LIMIT - 1,
                        hint = "От 0 до 100; пусто — любой балл",
                    ) {
                        binder.forField(this).bind({ draft -> draft.minScore }, { draft, value -> draft.minScore = value })
                    }
                    checkbox("Только ошибки", labelSize = 4, size = 8) {
                        binder.forField(this).bind({ draft -> draft.isErrorsOnly }, { draft, value -> draft.isErrorsOnly = value })
                    }
                }
                row {
                    dateRangeInput("Период отправки", labelSize = 4, size = 20) {
                        binder.forField(this).bind({ draft -> draft.period }, { draft, value -> draft.period = value })
                    }
                }
            }
        }
    }
    binder.readBean(SubmissionFiltersBuilder(defaults))
}

private fun ShowcaseSubmission.matches(criteria: SubmissionFilters): Boolean {
    val day = sentAt.toLocalDate()
    return author.contains(criteria.author, ignoreCase = true) &&
        (criteria.verdict == null || verdict == criteria.verdict) &&
        (criteria.minScore == null || (score != null && score >= criteria.minScore)) &&
        (!criteria.isErrorsOnly || verdict.isError) &&
        (criteria.period.from?.let { from -> day >= from } != false) &&
        (criteria.period.to?.let { to -> day <= to } != false)
}
