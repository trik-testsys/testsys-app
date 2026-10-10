package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteAlias
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.UserId
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.manager.ContestResultsVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.UiTexts

/** Columns of the identifier and nickname of a member in the results table. */
private const val MEMBER_COLUMNS = 8

/** Columns of one task in the results table. */
private const val TASK_COLUMNS = 3

/** Logical columns of the results table when its tasks fit the block. */
private const val MIN_RESULT_COLUMNS = 24

/**
 * Contest in a class or competition of a Manager (testsys.web.page.manager.contest): the contest details and the table of
 * the best results and submission counts of the students or participants by task, with its download as a CSV file.
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/classes/:classId([0-9]+)/contests/:contestId([0-9]+)")
@RouteAlias("manager/competitions/:competitionId([0-9]+)/contests/:contestId([0-9]+)")
@PageTitle("Тур")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerContestView(texts: UiTexts, private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val parameters = event.routeParameters
        val contestId = ContestId(parameters.getLong(MANAGER_CONTEST_ID_PARAMETER).orElseThrow())
        val classId = parameters.getLong(CLASS_ID_PARAMETER).map(::ClassId).orElse(null)
        val group = if (classId != null) {
            val results = { managerService.viewClassContest(classId, contestId) }
            Group(
                results = results(),
                fetch = results,
                name = "Класс «${managerService.viewClass(classId).studyClass.name}»",
                section = CLASSES_SECTION,
                parameters = classParameters(classId),
                target = ManagerClassView::class.java,
                filename = "contest-${contestId.value}-class-${classId.value}.csv",
            )
        } else {
            val competitionId = CompetitionId(parameters.getLong(COMPETITION_ID_PARAMETER).orElseThrow())
            val results = { managerService.viewCompetitionContest(competitionId, contestId) }
            Group(
                results = results(),
                fetch = results,
                name = "Соревнование «${managerService.viewCompetition(competitionId).competition.name}»",
                section = COMPETITIONS_SECTION,
                parameters = competitionParameters(competitionId),
                target = ManagerCompetitionView::class.java,
                filename = "contest-${contestId.value}-competition-${competitionId.value}.csv",
            )
        }

        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Тур «${group.results.contest.name}»") {
                managerCrumbs(group.section)
                crumb(group.name, group.target, group.parameters)
            }
            detailsBlock(group.results.contest)
            resultsBlock(group)
        }
    }

    private fun PageScope.detailsBlock(contest: ContestVo) {
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 4, size = 8) { value = contest.id.value.toString() }
                    textInput("Название", labelSize = 4, size = 8) { value = contest.name }
                }
                row {
                    dateTimeInput("Начало", labelSize = 4, size = 8) { value = contest.startsAt?.toServerDateTime() }
                    dateTimeInput("Конец", labelSize = 4, size = 8) { value = contest.endsAt?.toServerDateTime() }
                }
                row {
                    codeInput("Время на прохождение", labelSize = 4, size = 8) { value = formatDuration(contest.attemptDuration).orEmpty() }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.resultsBlock(group: Group) {
        val results = group.results
        val byMemberAndTask = results.results.associateBy { result -> result.authorId to result.taskId }
        row {
            block(title = "Результаты") {
                table(
                    key = { (memberId, _): Pair<UserId, String> -> memberId },
                    gridColumns = maxOf(MIN_RESULT_COLUMNS, MEMBER_COLUMNS + TASK_COLUMNS * results.tasks.size),
                    fetch = { request -> pageOf(results.members, request) },
                ) {
                    codeColumn("ID", size = TASK_COLUMNS) { (memberId, _) -> memberId.value.toString() }
                    textColumn("Псевдоним", size = MEMBER_COLUMNS - TASK_COLUMNS) { (_, name) -> name }
                    results.tasks.forEach { task ->
                        codeColumn("${task.name} · ${task.id.value}", size = TASK_COLUMNS) { (memberId, _) ->
                            val result = byMemberAndTask[memberId to task.id]
                            "${result?.bestScore?.value ?: "—"} · ${result?.submissionCount ?: 0}"
                        }
                    }
                    empty("Участников нет", "Строки появятся, когда в группе будут ученики или участники.")
                }
                actions {
                    downloadAction("Скачать CSV", produce = { context ->
                        val bytes = contestResultsCsv(group.fetch()).toByteArray(Charsets.UTF_8)
                        context.ensureActive()
                        DownloadContent(filename = group.filename, contentType = "text/csv; charset=UTF-8", length = bytes.size.toLong()) {
                            bytes.inputStream()
                        }
                    })
                }
            }
        }
    }

    /**
     * Class or competition of the shown contest: its current [results], their [fetch] for the download, the crumb [name] and
     * the page [target] with its [parameters], the [section] of the Cabinet and the CSV [filename].
     */
    private class Group(
        val results: ContestResultsVo,
        val fetch: () -> ContestResultsVo,
        val name: String,
        val section: String,
        val parameters: RouteParameters,
        val target: Class<out TestSysView>,
        val filename: String,
    )
}

/**
 * Returns [results] as CSV in UTF-8 with a byte order mark: columns separated by `;`, values quoted by RFC 4180, the identifier
 * and nickname of each member and the best result and submission count for each task; an absent best result is empty.
 */
internal fun contestResultsCsv(results: ContestResultsVo): String {
    val byMemberAndTask: Map<Pair<UserId, TaskId>, ContestTaskResult> =
        results.results.associateBy { result -> result.authorId to result.taskId }
    val header = listOf("ID", "Псевдоним") + results.tasks.flatMap { task -> listOf("${task.name} — балл", "${task.name} — посылок") }
    val rows = results.members.map { (memberId, name) ->
        listOf(memberId.value.toString(), name) + results.tasks.flatMap { task ->
            val result = byMemberAndTask[memberId to task.id]
            listOf(result?.bestScore?.value?.toString().orEmpty(), (result?.submissionCount ?: 0).toString())
        }
    }
    return csvOf(listOf(header) + rows)
}
