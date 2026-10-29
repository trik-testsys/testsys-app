package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.infra.localization.PageManager
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
import tech.testsys.web.components.texts.currentLocalization

/** Columns of the identifier and nickname of a member in the results table. */
private const val MEMBER_COLUMNS = 8

/** Columns of the identifier of a member in the results table: an 18-digit id fits on one line. */
private const val ID_COLUMNS = 4

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
@RolesAllowed("MULTIPLE_ROLE")
class ManagerContestView(private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageManager.contestTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val texts = currentLocalization().pageManager
        val parameters = event.routeParameters
        val contestId = ContestId(parameters.getLong(MANAGER_CONTEST_ID_PARAMETER).orElseThrow())
        val classId = parameters.getLong(CLASS_ID_PARAMETER).map(::ClassId).orElse(null)
        val group = if (classId != null) {
            val results = { managerService.viewClassContest(classId, contestId) }
            Group(
                results = results(),
                fetch = results,
                name = texts.classTitle(name = managerService.viewClass(classId).studyClass.name),
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
                name = texts.competitionTitle(name = managerService.viewCompetition(competitionId).competition.name),
                section = COMPETITIONS_SECTION,
                parameters = competitionParameters(competitionId),
                target = ManagerCompetitionView::class.java,
                filename = "contest-${contestId.value}-competition-${competitionId.value}.csv",
            )
        }

        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.contestTitle(name = group.results.contest.name)) {
                managerCrumbs(group.section)
                crumb(group.name, group.target, group.parameters)
            }
            guide(texts.contestGuide())
            detailsBlock(group.results.contest)
            resultsBlock(group)
        }
    }

    private fun PageScope.detailsBlock(contest: ContestVo) {
        val texts = currentLocalization().pageManager
        row {
            block(title = texts.detailsTitle()) {
                guide(texts.contestDetailsGuide())
                row {
                    codeInput(texts.id(), labelSize = 3, size = 9) { value = contest.id.value.toString() }
                    textInput(texts.name(), labelSize = 3, size = 9) { value = contest.name }
                }
                row {
                    dateTimeInput(texts.startsAt(), labelSize = 3, size = 9) { value = contest.startsAt?.toUserDateTime() }
                    dateTimeInput(texts.endsAt(), labelSize = 3, size = 9) { value = contest.endsAt?.toUserDateTime() }
                }
                row {
                    codeInput(texts.attemptDuration(), labelSize = 3, size = 9) {
                        value = formatDuration(contest.attemptDuration).orEmpty()
                    }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.resultsBlock(group: Group) {
        val texts = currentLocalization().pageManager
        val ui = UI.getCurrent()
        val results = group.results
        val byMemberAndTask = results.results.associateBy { result -> result.authorId to result.taskId }
        val taskColumns = TASK_COLUMNS * results.tasks.size
        // With few tasks the nickname takes the rest, so the table fills the block.
        val nicknameColumns = maxOf(MEMBER_COLUMNS - ID_COLUMNS, MIN_RESULT_COLUMNS - ID_COLUMNS - taskColumns)
        row {
            block(title = texts.contestResultsTitle()) {
                guide(texts.contestResultsGuide())
                table(
                    key = { (memberId, _): Pair<UserId, String> -> memberId },
                    gridColumns = maxOf(MIN_RESULT_COLUMNS, MEMBER_COLUMNS + taskColumns),
                    fetch = { request -> pageOf(results.members, request) },
                ) {
                    codeColumn(texts.id(), size = ID_COLUMNS) { (memberId, _) -> memberId.value.toString() }
                    textColumn(texts.nickname(), size = nicknameColumns) { (_, name) -> name }
                    results.tasks.forEach { task ->
                        codeColumn(texts.contestResultsTask(name = task.name, id = task.id.value), size = TASK_COLUMNS) { (memberId, _) ->
                            val result = byMemberAndTask[memberId to task.id]
                            val count = result?.submissionCount ?: 0
                            val score = result?.bestScore?.value
                            if (score == null) {
                                texts.contestResultsCellEmpty(count = count)
                            } else {
                                texts.contestResultsCell(score = score, count = count)
                            }
                        }
                    }
                    empty(texts.contestResultsEmpty(), texts.contestResultsEmptyHint())
                }
                actions {
                    downloadAction(texts.downloadCsv(), produce = { context ->
                        val fetched = group.fetch()
                        val csv = inUiOf(ui) { contestResultsCsv(fetched, currentLocalization().pageManager) }
                        val bytes = csv.toByteArray(Charsets.UTF_8)
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
 * Returns [results] as CSV in UTF-8 with a byte order mark: columns separated by `;`, values quoted by RFC 4180, the header
 * from [texts], the identifier and nickname of each member and the best result and submission count for each task; an absent
 * best result is empty.
 */
internal fun contestResultsCsv(results: ContestResultsVo, texts: PageManager): String {
    val byMemberAndTask: Map<Pair<UserId, TaskId>, ContestTaskResult> =
        results.results.associateBy { result -> result.authorId to result.taskId }
    val header = listOf(texts.id(), texts.nickname()) + results.tasks.flatMap { task ->
        listOf(texts.contestCsvScore(task = task.name), texts.contestCsvSubmissions(task = task.name))
    }
    val rows = results.members.map { (memberId, name) ->
        listOf(memberId.value.toString(), name) + results.tasks.flatMap { task ->
            val result = byMemberAndTask[memberId to task.id]
            listOf(result?.bestScore?.value?.toString().orEmpty(), (result?.submissionCount ?: 0).toString())
        }
    }
    return csvOf(listOf(header) + rows)
}
