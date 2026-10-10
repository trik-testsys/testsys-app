package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.texts.UiTexts
import java.time.Clock
import java.time.Instant

/**
 * Participant cabinet with the contests of their competition and separate remaining-time cells.
 *
 * @since %CURRENT_VERSION%
 */
@Route("participant/:section?(contests)")
@PageTitle("Кабинет Участника")
@RolesAllowed("PARTICIPANT")
class ParticipantView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val participantService: ParticipantService,
    private val clock: Clock,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val contests = participantService.viewContests()
        val now = clock.instant()
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            head("Кабинет Участника")
            row {
                block(title = "Туры") {
                    table(
                        key = { (_, contest): Pair<Instant?, ContestVo> -> contest.id },
                        fetch = { request -> pageOf(contests, request) },
                    ) {
                        textColumn("Название", size = 5) { (_, contest) -> contest.name }
                        dateTimeColumn("Начало", size = 4) { (_, contest) -> contest.startsAt?.toServerDateTime() }
                        dateTimeColumn("Конец", size = 4) { (_, contest) -> contest.endsAt?.toServerDateTime() }
                        textColumn("Время на прохождение", size = 3) { (_, contest) -> studyDurationText(contest.attemptDuration) }
                        column("Состояние", size = 4) { (enteredAt, contest) -> studyState(contest, enteredAt, now) }
                        column("Осталось") { (enteredAt, contest) -> studyTimer(contest, enteredAt, now) }
                        empty("Туров пока нет", "Туры появятся, когда Организатор добавит их в соревнование.")
                        onRowClick(isNavigation = true) { (_, contest) ->
                            UI.getCurrent().navigate(
                                ParticipantContestView::class.java,
                                RouteParameters(STUDY_CONTEST_ID_PARAMETER, contest.id.value.toString()),
                            )
                        }
                    }
                }
            }
        }
    }
}
