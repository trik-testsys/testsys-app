package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
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
import tech.testsys.web.components.texts.UiTexts

/**
 * Observer cabinet with current competition and assigned contest pairs and a results download.
 *
 * @since %CURRENT_VERSION%
 */
@Route("observer/:section?(contests)")
@PageTitle("Кабинет Наблюдателя")
@RolesAllowed("OBSERVER")
class ObserverView(texts: UiTexts, private val headers: CabinetHeaders, private val service: ObserverService) :
    TestSysView(texts),
    BeforeEnterObserver {

    override fun beforeEnter(event: BeforeEnterEvent) {
        service.viewContests(Pagination(0, 1))
        var applied = ObserverContestFilter()
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            head("Кабинет Наблюдателя")
            row {
                block(title = "Туры Соревнований") {
                    val rows = table(
                        key = { pair: tech.testsys.web.app.service.observer.ObserverContestVo ->
                            pair.competition.id to pair.contest.id
                        },
                        fetch = { request ->
                            val result = service.viewContests(request.toPagination(), applied)
                            Page(result.content, result.totalElements.toInt())
                        },
                    ) {
                        textColumn("Соревнование", sortKey = "competitionName", size = 5) { it.competition.name }
                        textColumn("Тур", sortKey = "name", size = 5) { it.contest.name }
                        dateTimeColumn("Начало", size = 4) { it.contest.startsAt?.toServerDateTime() }
                        dateTimeColumn("Конец", size = 4) { it.contest.endsAt?.toServerDateTime() }
                        textColumn("Время на прохождение", size = 4) { studyDurationText(it.contest.attemptDuration) }
                        empty("Туров пока нет", "Назначенные Туры появятся здесь вместе с Соревнованиями.")
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
                            competitionName = textInput("Соревнование", labelSize = 4, size = 8)
                            contestName = textInput("Тур", labelSize = 4, size = 8)
                        }
                        row {
                            competitionId = textInput("ID Соревнования", labelSize = 4, size = 8)
                            contestId = textInput("ID Тура", labelSize = 4, size = 8)
                        }
                    }
                    actions {
                        downloadAction("Скачать все результаты", produce = { context ->
                            val result = service.downloadResult()
                            val bytes = observerResultsCsv(result).toByteArray(Charsets.UTF_8)
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

/** Serializes [result] with observer column labels and CSV escaping. */
internal fun observerResultsCsv(result: ObserverResult): String {
    val header = listOf("ID участника") + result.columns.map { column ->
        "${column.contestName} - ${column.taskName}"
    }
    val rows = result.rows.map { row ->
        listOf(row.participantId.value.toString()) + row.scores.map { score -> score?.toString().orEmpty() }
    }
    return csvOf(listOf(header) + rows)
}
