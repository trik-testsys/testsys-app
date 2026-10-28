package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.web.app.service.manager.CompetitionDetailsVo
import tech.testsys.web.app.service.manager.IssuedParticipantVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.app.service.manager.ParticipantVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import java.time.Instant

/**
 * Competition of a Manager (testsys.web.page.manager.competition): its details, participants with their creation and
 * deletion and contests with adding a contest; a contest opens its results in the competition.
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/competitions/:competitionId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerCompetitionView(private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageManager.competitionTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show(CompetitionId(event.routeParameters.getLong(COMPETITION_ID_PARAMETER).orElseThrow()))
    }

    private fun show(competitionId: CompetitionId) {
        val details = managerService.viewCompetition(competitionId)
        val texts = currentLocalization().pageManager
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.competitionTitle(name = details.competition.name)) { managerCrumbs(COMPETITIONS_SECTION) }
            guide(texts.competitionGuide())
            detailsBlock(details)
            participantsBlock(competitionId, details.participants)
            managerContestsBlock(
                details.contests,
                managerService,
                onOpen = { contest -> openCompetitionContest(competitionId, contest.id) },
                onAdd = { contest ->
                    managerService.addCompetitionContest(competitionId, contest.id)
                    toast(FeedbackKind.Success, currentLocalization().pageManager.competitionContestAdded())
                    show(competitionId)
                },
            )
        }
    }

    private fun PageScope.detailsBlock(details: CompetitionDetailsVo) {
        val texts = currentLocalization().pageManager
        val entity = details.competition
        val draft = Binder<DetailsDraft>()
        row {
            block(title = texts.detailsTitle()) {
                guide(texts.detailsGuide())
                editing(
                    onSave = {
                        val values = DetailsDraft()
                        val isValid = draft.writeBeanIfValid(values)
                        if (isValid) {
                            managerService.editCompetition(
                                competitionId = entity.id,
                                competitionName = values.name,
                                description = values.description,
                            )
                            toast(FeedbackKind.Success, currentLocalization().pageManager.competitionSaved())
                            show(entity.id)
                        }
                        isValid
                    },
                    onCancel = { draft.readBean(DetailsDraft(entity.name, entity.description)) },
                )
                row {
                    codeInput(texts.id(), labelSize = 6, size = 18) {
                        value = entity.id.value.toString()
                        isEditable = false
                    }
                }
                row {
                    textInput(texts.name(), labelSize = 6, size = 18) {
                        draft.forField(this).nameRules(texts.nameRequired())
                            .bind({ values -> values.name }, { values, name -> values.name = name })
                    }
                }
                row {
                    textArea(texts.description(), labelSize = 6, size = 18, maxLines = 4) {
                        draft.forField(this)
                            .bind({ values -> values.description }, { values, description -> values.description = description })
                    }
                }
                row {
                    codeInput(texts.participantCount(), labelSize = 6, size = 6) {
                        value = details.participants.size.toString()
                        isEditable = false
                    }
                    codeInput(texts.contestCount(), labelSize = 6, size = 6) {
                        value = details.contests.size.toString()
                        isEditable = false
                    }
                }
            }
        }

        draft.readBean(DetailsDraft(entity.name, entity.description))
    }

    /** Shows the just [created] participants once with their CSV: their access codes cannot be read back later. */
    private fun issuedParticipantsDialog(competitionId: CompetitionId, created: List<IssuedParticipantVo>) {
        val localization = currentLocalization()
        val texts = localization.pageManager
        val ui = UI.getCurrent()
        dialog(title = texts.competitionParticipantsIssuedTitle(), subtitle = texts.competitionParticipantsIssuedGuide()) {
            row {
                alert(
                    kind = FeedbackKind.Warning,
                    title = texts.competitionParticipantsIssuedAlertTitle(),
                    text = texts.competitionParticipantsIssuedAlertText(),
                )
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogClose()) { onClick { dialog.close() } }
                downloadAction(texts.downloadCsv(), produce = { context ->
                    val csv = inUiOf(ui) { participantsCsv(created, currentLocalization().pageManager) }
                    val bytes = csv.toByteArray(Charsets.UTF_8)
                    context.ensureActive()
                    DownloadContent(
                        filename = "competition-${competitionId.value}-new-participants.csv",
                        contentType = "text/csv; charset=UTF-8",
                        length = bytes.size.toLong(),
                    ) { bytes.inputStream() }
                })
            }
        }.open()
    }

    private fun PageScope.participantsBlock(competitionId: CompetitionId, participants: List<Pair<ParticipantVo, Instant?>>) {
        val texts = currentLocalization().pageManager
        row {
            block(title = texts.competitionParticipantsTitle()) {
                guide(texts.competitionParticipantsGuide())
                val rows = table(
                    key = { (participant, _): Pair<ParticipantVo, Instant?> -> participant.id },
                    selectable = true,
                    fetch = { request -> pageOf(participants, request) },
                ) {
                    codeColumn(texts.id(), size = 5) { (participant, _) -> participant.id.value.toString() }
                    textColumn(texts.nickname(), size = 11) { (participant, _) -> participant.name }
                    dateTimeColumn(texts.lastLogin()) { (_, lastLogin) -> lastLogin?.toUserDateTime() }
                    val actionsLabel = { (participant, _): Pair<ParticipantVo, Instant?> ->
                        texts.competitionParticipantsActions(name = participant.name)
                    }
                    menuColumn(ariaLabel = actionsLabel) { (participant, _) ->
                        destructiveItem(texts.competitionParticipantsDelete()) { deleteParticipant(competitionId, participant) }
                    }
                    empty(texts.competitionParticipantsEmpty(), texts.competitionParticipantsEmptyHint())
                }
                val creation = participantsDialog(competitionId)
                actions {
                    action(texts.competitionParticipantsCreate()) { onClick { creation() } }
                    destructiveAction(texts.competitionParticipantsDeleteSelected()) {
                        bindEnabled(rows.selection.map { selected -> selected.isNotEmpty() })
                        onClick { deleteSelected(competitionId, rows) }
                    }
                }
            }
        }
    }

    private fun deleteParticipant(competitionId: CompetitionId, participant: ParticipantVo) {
        val texts = currentLocalization().pageManager
        confirm(
            title = texts.competitionParticipantsDeleteTitle(name = participant.name),
            text = texts.competitionParticipantsDeleteText(),
            action = texts.competitionParticipantsDelete(),
            isDanger = true,
        ) {
            managerService.deleteParticipants(competitionId, setOf(participant.id))
            toast(FeedbackKind.Success, currentLocalization().pageManager.competitionParticipantsDeleted())
            show(competitionId)
        }
    }

    private fun deleteSelected(competitionId: CompetitionId, rows: TableHandle<Pair<ParticipantVo, Instant?>>) {
        val texts = currentLocalization().pageManager
        val participantIds = rows.selected.filterIsInstance<SingleRoleUserId>().toSet()
        confirm(
            title = texts.competitionParticipantsDeleteSelectedTitle(count = participantIds.size),
            text = texts.competitionParticipantsDeleteSelectedText(),
            action = texts.competitionParticipantsDelete(),
            isDanger = true,
        ) {
            val deleted = managerService.deleteParticipants(competitionId, participantIds)
            rows.clearSelection()
            toast(FeedbackKind.Success, currentLocalization().pageManager.competitionParticipantsDeletedSelected(count = deleted.size))
            show(competitionId)
        }
    }

    /** Builds the participant creation dialog of [competitionId] and returns its opening, which starts with an empty form. */
    private fun participantsDialog(competitionId: CompetitionId): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageManager
        val draft = Binder<CountDraft>()
        val title = texts.competitionParticipantsDialogTitle()
        val creation = dialog(title = title, subtitle = texts.competitionParticipantsDialogSubtitle()) {
            row {
                integerInput(texts.competitionParticipantsCount(), labelSize = 6, size = 18, min = 1) {
                    draft.forField(this)
                        .asRequired(texts.competitionParticipantsCountRequired())
                        .bind({ values -> values.count }, { values, count -> values.count = count })
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.create()) {
                    onClick {
                        val values = CountDraft()
                        if (draft.writeBeanIfValid(values)) {
                            val count = checkNotNull(values.count)
                            val created = managerService.createParticipants(competitionId, count)
                            val messages = currentLocalization().pageManager
                            toast(
                                FeedbackKind.Success,
                                messages.competitionParticipantsCreated(),
                                messages.competitionParticipantsCreatedTotal(count = count),
                            )
                            dialog.close()
                            show(competitionId)
                            issuedParticipantsDialog(competitionId, created)
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

    private class DetailsDraft(var name: String = "", var description: String = "")
}
