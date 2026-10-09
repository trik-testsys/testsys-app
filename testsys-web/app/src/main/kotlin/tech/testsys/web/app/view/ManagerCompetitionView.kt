package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.web.app.service.manager.CompetitionDetailsVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.app.service.manager.ParticipantVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts
import java.time.Instant

/**
 * Competition of a Manager (testsys.web.page.manager.competition): its details, participants with their creation and
 * deletion and contests with adding a contest; a contest opens its results in the competition.
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/competitions/:competitionId([0-9]+)")
@PageTitle("Соревнование")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerCompetitionView(texts: UiTexts, private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        show(CompetitionId(event.routeParameters.getLong(COMPETITION_ID_PARAMETER).orElseThrow()))
    }

    private fun show(competitionId: CompetitionId) {
        val details = managerService.viewCompetition(competitionId)
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Соревнование «${details.competition.name}»") { managerCrumbs(COMPETITIONS_SECTION) }
            detailsBlock(details)
            participantsBlock(competitionId, details.participants)
            managerContestsBlock(
                details.contests,
                managerService,
                onOpen = { contest -> openCompetitionContest(competitionId, contest.id) },
                onAdd = { contest ->
                    managerService.addCompetitionContest(competitionId, contest.id)
                    show(competitionId)
                },
            )
        }
    }

    private fun PageScope.detailsBlock(details: CompetitionDetailsVo) {
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 3, size = 6) { value = details.competition.id.value.toString() }
                    textInput("Название", labelSize = 3, size = 12) { value = details.competition.name }
                }
                row {
                    codeInput("Участников", labelSize = 3, size = 6) { value = details.participants.size.toString() }
                    codeInput("Туров", labelSize = 3, size = 6) { value = details.contests.size.toString() }
                }
            }.isEditable = false
        }
    }

    @RawAccessTokenDependency(reason = "Shows the stored access codes of the participants as the issued ones.")
    private fun PageScope.participantsBlock(competitionId: CompetitionId, participants: List<Pair<ParticipantVo, Instant?>>) {
        row {
            block(title = "Участники") {
                table(
                    key = { (participant, _): Pair<ParticipantVo, Instant?> -> participant.id },
                    fetch = { request -> pageOf(participants, request) },
                ) {
                    codeColumn("ID", size = 4) { (participant, _) -> participant.id.value.toString() }
                    textColumn("Псевдоним", size = 6) { (participant, _) -> participant.name }
                    dateTimeColumn("Последний вход", size = 5) { (_, lastLogin) -> lastLogin?.toServerDateTime() }
                    codeColumn("Код-доступа", isObscured = true) { (participant, _) -> participant.accessTokenHash.value }
                    menuColumn(ariaLabel = { (participant, _) -> "Действия с участником «${participant.name}»" }) { (participant, _) ->
                        destructiveItem("Удалить") { deleteParticipant(competitionId, participant) }
                    }
                    empty("Участников пока нет", "Создайте участников, чтобы раздать им коды-доступа.")
                }
                val creation = participantsDialog(competitionId)
                actions { action("Создать участников") { onClick { creation() } } }
            }
        }
    }

    private fun deleteParticipant(competitionId: CompetitionId, participant: ParticipantVo) {
        confirm(
            title = "Удалить участника «${participant.name}»?",
            text = "Код-доступа участника перестанет действовать. Участника с посылками удалить нельзя.",
            action = "Удалить",
            isDanger = true,
        ) {
            managerService.deleteParticipant(competitionId, participant.id)
            show(competitionId)
        }
    }

    /** Builds the participant creation dialog of [competitionId] and returns its opening, which starts with an empty form. */
    private fun participantsDialog(competitionId: CompetitionId): () -> Unit {
        val draft = Binder<CountDraft>()
        val creation = dialog(title = "Новые участники", subtitle = "Каждый участник получит свой Код-доступа") {
            row {
                integerInput("Количество", labelSize = 6, size = 18, min = 1) {
                    draft.forField(this)
                        .asRequired("Укажите количество")
                        .bind({ values -> values.count }, { values, count -> values.count = count })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Создать") {
                    onClick {
                        val values = CountDraft()
                        if (draft.writeBeanIfValid(values)) {
                            managerService.createParticipants(competitionId, checkNotNull(values.count))
                            dialog.close()
                            show(competitionId)
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(CountDraft())
            creation.open()
        }
    }

    /** Values of the participant creation form. */
    private class CountDraft(var count: Int? = null)
}
