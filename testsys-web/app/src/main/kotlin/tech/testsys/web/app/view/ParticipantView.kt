package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.texts.currentLocalization
import java.time.Clock
import java.time.Instant

/**
 * Participant cabinet with the contests of their competition and separate remaining-time cells.
 *
 * @since 1.0.0-beta.1
 */
@Route("participant/:section?(contests)")
@RolesAllowed("PARTICIPANT")
class ParticipantView(
    private val headers: CabinetHeaders,
    private val participantService: ParticipantService,
    private val clock: Clock,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageParticipant.title()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val contests = participantService.viewContests()
        val now = clock.instant()
        val localization = currentLocalization()
        val texts = localization.pageParticipant
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            head(texts.title())
            guide(texts.guide())
            row {
                block(title = localization.pageCabinet.sectionContests()) {
                    guide(texts.contestsGuide())
                    table(
                        key = { (_, contest): Pair<Instant?, ContestVo> -> contest.id },
                        fetch = { request -> pageOf(contests, request) },
                    ) {
                        textColumn(texts.name(), size = 5) { (_, contest) -> contest.name }
                        dateTimeColumn(texts.startsAt(), size = 4) { (_, contest) -> contest.startsAt?.toUserDateTime() }
                        dateTimeColumn(texts.endsAt(), size = 4) { (_, contest) -> contest.endsAt?.toUserDateTime() }
                        textColumn(texts.attemptDuration(), size = 3) { (_, contest) -> studyDurationText(contest.attemptDuration) }
                        column(texts.contestStateTitle(), size = 4) { (enteredAt, contest) ->
                            studyState(contest, enteredAt, now, ParticipantStudyTexts)
                        }
                        column(texts.remainingTitle()) { (enteredAt, contest) ->
                            studyTimer(contest, enteredAt, now, ParticipantStudyTexts)
                        }
                        empty(texts.contestsEmpty(), texts.contestsEmptyHint())
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
