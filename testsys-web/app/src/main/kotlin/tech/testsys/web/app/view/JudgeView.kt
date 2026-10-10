package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.web.app.service.judge.JudgeResultVo
import tech.testsys.web.app.service.judge.JudgeService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.domain.contract.persistence.Sort as DomainSort

/** Route parameter of [JudgeView] with the open section. */
internal const val JUDGE_SECTION_PARAMETER = "section"

/** Route parameter of [JudgeSolutionView] with the submission id. */
internal const val SUBMISSION_ID_PARAMETER = "submissionId"

private const val SUBMISSIONS_SECTION = "submissions"

/** Verdicts of the table, the latest first. */
private val LATEST_FIRST = DomainSort(
    listOf(DomainSort.Order("createdAt", DomainSort.Direction.DESC), DomainSort.Order("id", DomainSort.Direction.DESC)),
)

/** Returns the route parameters of the submissions section of [JudgeView]. */
internal fun judgeSubmissionsParameters(): RouteParameters = RouteParameters(JUDGE_SECTION_PARAMETER, SUBMISSIONS_SECTION)

/** Opens the page of the submission [submissionId] for a judge. */
internal fun openJudgeSolution(submissionId: SubmissionId) {
    UI.getCurrent().navigate(JudgeSolutionView::class.java, RouteParameters(SUBMISSION_ID_PARAMETER, submissionId.value.toString()))
}

/**
 * Cabinet of a Judge (testsys.web.page.judge): the verdicts of the submissions of students and participants, paged
 * and filtered by author, submission, class and competition; a row opens the submission. Without a section the page
 * opens «Посылки».
 *
 * @since %CURRENT_VERSION%
 */
@Route("judge/:section?(submissions)")
@PageTitle("Кабинет Судьи")
@RolesAllowed("MULTIPLE_ROLE")
class JudgeView(texts: UiTexts, private val headers: CabinetHeaders, private val judgeService: JudgeService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (event.routeParameters.get(JUDGE_SECTION_PARAMETER).isEmpty) {
            event.forwardTo(JudgeView::class.java, judgeSubmissionsParameters())
            return
        }

        // Checks the judge role before the table loads, so that its absence opens the forbidden screen.
        judgeService.viewResults(Pagination(page = 0, size = 1))
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Кабинет Судьи") { crumb("Главная", MultiMainView::class.java) }
            row { block(title = "Посылки") { submissionsTable() } }
        }
    }

    private fun BlockScope.submissionsTable() {
        var applied = VerdictFilter()
        val rows = table(
            key = { result: JudgeResultVo -> result.verdict.id },
            fetch = { request ->
                val pageNumber = request.offset / request.limit
                val results = judgeService.viewResults(Pagination(page = pageNumber, size = request.limit, sort = LATEST_FIRST), applied)
                Page(results.content, results.totalElements.toInt())
            },
        ) {
            codeColumn("Посылка", size = 4) { result -> result.submission.id.value.toString() }
            textColumn("Автор", size = 5) { result -> result.author.name }
            dateTimeColumn("Отправлено", size = 4) { result -> result.submission.createdAt.toServerDateTime() }
            numberColumn("Автоматический балл", size = 4) { result ->
                result.verdict.testVerdicts.sumOf { outcome -> outcome.score.value.toLong() }
            }
            numberColumn("Судейский балл", size = 3) { result -> result.lastJudgment?.score?.value }
            numberColumn("Итоговый балл") { result -> result.finalScore }
            empty("Посылок нет", "Здесь появятся успешно проверенные Посылки Учеников и Участников.")
            onRowClick(isNavigation = true) { result -> openJudgeSolution(result.submission.id) }
        }

        val draft = Binder<VerdictFilterDraft>()
        filters(
            onApply = {
                val values = VerdictFilterDraft()
                val isValid = draft.writeBeanIfValid(values)
                if (isValid) applied = values.toFilter()
                isValid
            },
            onReset = {
                draft.readBean(VerdictFilterDraft())
                applied = VerdictFilter()
            },
            onRefresh = { rows.refresh(toFirstPage = true) },
        ) {
            row {
                codeInput("ID автора", labelSize = 4, size = 8) {
                    bindId(draft, { values -> values.author }, { values, id -> values.author = id })
                }
                codeInput("ID посылки", labelSize = 4, size = 8) {
                    bindId(draft, { values -> values.submission }, { values, id -> values.submission = id })
                }
            }
            row {
                codeInput("ID класса", labelSize = 4, size = 8) {
                    bindId(draft, { values -> values.group }, { values, id -> values.group = id })
                }
                codeInput("ID соревнования", labelSize = 4, size = 8) {
                    bindId(draft, { values -> values.competition }, { values, id -> values.competition = id })
                }
            }
        }
        draft.readBean(VerdictFilterDraft())
    }

    /** Binds this identifier field to [draft], accepting an empty value or a positive integer. */
    private fun ValueInput<String>.bindId(
        draft: Binder<VerdictFilterDraft>,
        getter: (VerdictFilterDraft) -> String,
        setter: (VerdictFilterDraft, String) -> Unit,
    ) {
        draft.forField(this)
            .withValidator({ value -> value.isBlank() || idOf(value) != null }, "Введите положительное целое число")
            .bind(getter, setter)
    }

    /** Values of the filter form of the verdicts table. */
    private class VerdictFilterDraft(
        var author: String = "",
        var submission: String = "",
        var group: String = "",
        var competition: String = "",
    ) {
        fun toFilter(): VerdictFilter = VerdictFilter(
            // Users of every kind share one id sequence and the filter compares raw values, so the kind of the id is irrelevant.
            authorId = idOf(author)?.let(::MultipleRoleUserId),
            submissionId = idOf(submission)?.let(::SubmissionId),
            classId = idOf(group)?.let(::ClassId),
            competitionId = idOf(competition)?.let(::CompetitionId),
        )
    }
}

/** Returns the positive identifier written in [value], or `null` if it is empty or not a positive integer. */
private fun idOf(value: String): Long? = value.trim().toLongOrNull()?.takeIf { id -> id > 0 }
