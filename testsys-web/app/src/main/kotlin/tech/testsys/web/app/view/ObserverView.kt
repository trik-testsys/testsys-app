package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.infra.localization.PageObserver
import tech.testsys.operation.user.ObserverResult
import tech.testsys.web.app.service.observer.ObserverService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Observer cabinet with current competition and assigned contest pairs and a results download.
 *
 * @since 1.0.0-beta.1
 */
@Route("observer/:section?(contests)")
@RolesAllowed("OBSERVER")
class ObserverView(private val headers: CabinetHeaders, private val service: ObserverService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageObserver.title()

    override fun beforeEnter(event: BeforeEnterEvent) {
        service.viewContests(Pagination(0, 1))
        var applied = ObserverContestFilter()
        val ui = UI.getCurrent()
        val texts = currentLocalization().pageObserver
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            head(texts.title())
            guide(texts.guide())
            row {
                block(title = texts.contestsTitle()) {
                    guide(texts.contestsGuide())
                    val rows = table(
                        key = { pair: tech.testsys.web.app.service.observer.ObserverContestVo ->
                            pair.competition.id to pair.contest.id
                        },
                        fetch = { request ->
                            val result = service.viewContests(request.toPagination(), applied)
                            Page(result.content, result.totalElements.toInt())
                        },
                    ) {
                        textColumn(texts.competition(), sortKey = "competitionName", size = 6) { it.competition.name }
                        textColumn(texts.contest(), sortKey = "name", size = 6) { it.contest.name }
                        dateTimeColumn(texts.startsAt(), size = 4) { it.contest.startsAt?.toUserDateTime() }
                        dateTimeColumn(texts.endsAt(), size = 4) { it.contest.endsAt?.toUserDateTime() }
                        textColumn(texts.attemptDuration()) { studyDurationText(it.contest.attemptDuration) }
                        empty(texts.contestsEmpty(), texts.contestsEmptyHint())
                    }

                    lateinit var competitionName: ValueInput<String>
                    lateinit var contestName: ValueInput<String>
                    lateinit var competitionId: ValueInput<String>
                    lateinit var contestId: ValueInput<String>
                    filters(
                        onApply = {
                            val competitionNumber = competitionId.value.takeIf { it.isNotBlank() }?.toLongOrNull()
                            val contestNumber = contestId.value.takeIf { it.isNotBlank() }?.toLongOrNull()
                            val isValid = (competitionId.value.isBlank() || competitionNumber != null) &&
                                (contestId.value.isBlank() || contestNumber != null)
                            if (isValid) {
                                applied = ObserverContestFilter(
                                    name = contestName.value.ifEmpty { null },
                                    contestId = contestNumber?.let(::ContestId),
                                    competitionName = competitionName.value.ifEmpty { null },
                                    competitionId = competitionNumber?.let(::CompetitionId),
                                )
                            }
                            isValid
                        },
                        onReset = {
                            competitionName.value = ""
                            contestName.value = ""
                            competitionId.value = ""
                            contestId.value = ""
                            applied = ObserverContestFilter()
                        },
                        onRefresh = { rows.refresh(toFirstPage = true) },
                    ) {
                        row {
                            competitionName = textInput(texts.competition(), labelSize = 3, size = 9)
                            contestName = textInput(texts.contest(), labelSize = 3, size = 9)
                        }
                        row {
                            competitionId = textInput(texts.filterCompetitionId(), labelSize = 3, size = 9)
                            contestId = textInput(texts.filterContestId(), labelSize = 3, size = 9)
                        }
                    }
                    actions {
                        downloadAction(texts.contestsDownload(), produce = { context ->
                            val result = service.downloadResult()
                            val csv = inUiOf(ui) { observerResultsCsv(result, currentLocalization().pageObserver) }
                            val bytes = csv.toByteArray(Charsets.UTF_8)
                            context.ensureActive()
                            DownloadContent(
                                "observer-results.csv",
                                "text/csv; charset=UTF-8",
                                bytes.size.toLong(),
                            ) {
                                bytes.inputStream()
                            }
                        })
                    }
                }
            }
        }
    }
}

/**
 * Serializes [result] with CSV escaping: the participant identifier column and a «Contest - Task» column per task, with the
 * header from [texts].
 */
internal fun observerResultsCsv(result: ObserverResult, texts: PageObserver): String {
    val header = listOf(texts.csvParticipantId()) + result.columns.map { column ->
        texts.csvTask(contest = column.contestName, task = column.taskName)
    }
    val rows = result.rows.map { row ->
        listOf(row.participantId.value.toString()) + row.scores.map { score -> score?.toString().orEmpty() }
    }
    return csvOf(listOf(header) + rows)
}
