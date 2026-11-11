package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.app.service.administrator.CommunityInviteVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.lookupMany
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization

/** Columns of the community details block; the invite codes take the rest of the row beside it. */
private const val DETAILS_COLUMNS = 12

/**
 * Community of an Administrator (testsys.web.page.admin.community): its name and description with their editing, its
 * invite codes, its users and creation of its observers; right after creating an observer it shows its access code once.
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/communities/:communityId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class AdminCommunityView(private val headers: CabinetHeaders, private val administratorService: AdministratorService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageAdministrator.communityTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show(CommunityId(event.routeParameters.getLong(COMMUNITY_ID_PARAMETER).orElseThrow()), observer = null)
    }

    /** Shows [communityId], with the access code of the just created [observer] if given. */
    private fun show(communityId: CommunityId, observer: ObserverVo?) {
        // Checks the role, the community and its owner before the blocks load, so that a refusal opens its error screen.
        val community = administratorService.checkCommunityAccess(communityId)
        val localization = currentLocalization()
        val texts = localization.pageAdministrator
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.communityHead(name = community.name)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), AdminView::class.java)
                crumb(
                    localization.pageCabinet.sectionCommunities(),
                    AdminView::class.java,
                    RouteParameters(ADMIN_SECTION_PARAMETER, "communities"),
                )
            }
            guide(texts.communityGuide())
            row {
                detailsBlock(communityId) { show(communityId, observer) }
                invitesBlock(communityId)
            }
            observer?.let { created -> createdObserverBlock(created) }
            row {
                block(title = texts.communityUsersTitle()) {
                    guide(texts.communityUsersGuide())
                    // The community is fixed, so the community filter is hidden and offers only it.
                    adminUsersTable(administratorService, listOf(community), communityId)
                    val creation = observerDialog(communityId) { created -> show(communityId, created) }
                    actions { action(texts.communityObserverCreate()) { onClick { creation() } } }
                }
            }
        }
    }

    /** Loads the name and description of [communityId] for viewing; a saved change rebuilds the page through [onEdited]. */
    private fun PageRowScope.detailsBlock(communityId: CommunityId, onEdited: () -> Unit) {
        val texts = currentLocalization().pageAdministrator
        var form: DetailsForm? = null
        block(size = DETAILS_COLUMNS, title = texts.details()) {
            guide(texts.communityDetailsGuide())
            editing(onSave = { form?.save?.invoke() == true }, onCancel = { form?.cancel?.invoke() })
            load({ administratorService.checkCommunityAccess(communityId) }) { community -> form = detailsRows(community, onEdited) }
        }
    }

    /** Adds the rows of the name and description of [community] and returns their form. */
    private fun BlockScope.detailsRows(community: CommunityVo, onEdited: () -> Unit): DetailsForm {
        val texts = currentLocalization().pageAdministrator
        val draft = Binder<CommunityDraft>()
        row {
            codeInput(texts.id(), labelSize = 6, size = 18) {
                value = community.id.value.toString()
                isEditable = false
            }
        }
        row {
            textInput(texts.name(), labelSize = 6, size = 18) {
                draft.forField(this)
                    .nameRules(texts.nameRequired())
                    .bind({ values -> values.name }, { values, name -> values.name = name })
            }
        }
        row {
            textArea(texts.description(), labelSize = 6, size = 18, maxLines = 4) {
                draft.forField(this)
                    .bind({ values -> values.description }, { values, description -> values.description = description })
            }
        }

        draft.readBean(CommunityDraft(community.name, community.description))
        return DetailsForm(
            save = {
                val values = CommunityDraft()
                val isValid = draft.writeBeanIfValid(values)
                if (isValid) {
                    administratorService.editCommunity(
                        communityId = community.id,
                        communityName = values.name,
                        description = values.description,
                    )
                    onEdited()
                    toast(FeedbackKind.Success, currentLocalization().pageAdministrator.communityEdited())
                }
                isValid
            },
            cancel = { draft.readBean(CommunityDraft(community.name, community.description)) },
        )
    }

    @RawAccessTokenDependency(reason = "Shows the stored access code of the created observer as the issued one.")
    private fun PageScope.createdObserverBlock(observer: ObserverVo) {
        val texts = currentLocalization().pageAdministrator
        row {
            block(title = texts.communityObserverCreatedTitle(name = observer.name)) {
                guide(texts.communityObserverCreatedGuide())
                row {
                    alert(
                        kind = FeedbackKind.Warning,
                        title = texts.communityObserverAlertTitle(),
                        text = texts.communityObserverAlertText(),
                    )
                }
                row {
                    codeInput(texts.accessCode(), labelSize = 6, size = 18) {
                        value = observer.accessTokenHash.value
                        isEditable = false
                        isObscured = true
                    }
                }
                footer { linkAction(texts.communityObserverOpen()) { onClick { openAdminUser(observer) } } }
            }
        }
    }

    @RawInviteCodeDependency(reason = "Shows the stored invite codes as the issued ones.")
    private fun PageRowScope.invitesBlock(communityId: CommunityId) {
        val texts = currentLocalization().pageAdministrator
        block(title = texts.communityInvitesTitle()) {
            guide(texts.communityInvitesGuide())
            // The menu of a row refreshes the table that contains it.
            lateinit var rows: LoadedTable<CommunityInviteVo>
            rows = loadTable(
                key = { invite: CommunityInviteVo -> invite.kind },
                fetch = { request -> pageOf(administratorService.viewCommunityInvites(communityId), request) },
            ) {
                column(texts.communityInvitesRole(), size = 6) { invite -> tag(roleOf(invite.kind)) }
                codeColumn(texts.communityInvitesCode(), size = 7, isObscured = true) { invite -> invite.codeHash.value }
                dateTimeColumn(texts.communityInvitesExpires()) { invite -> invite.expiresAt.toUserDateTime() }
                menuColumn(ariaLabel = { invite -> texts.communityInvitesActions(role = roleOf(invite.kind)) }, size = 2) { invite ->
                    item(texts.communityInvitesReplace()) {
                        confirm(
                            title = texts.communityInvitesReplaceTitle(),
                            text = texts.communityInvitesReplaceText(),
                            action = texts.communityInvitesReplace(),
                            isDanger = true,
                        ) {
                            administratorService.createCommunityInvite(communityId, invite.kind)
                            toast(FeedbackKind.Success, currentLocalization().pageAdministrator.communityInvitesReplaced())
                            rows.refresh()
                        }
                    }
                    item(texts.communityInvitesExtend()) {
                        administratorService.extendCommunityInvite(communityId, invite.kind)
                        toast(FeedbackKind.Success, currentLocalization().pageAdministrator.communityInvitesExtended())
                        rows.refresh()
                    }
                }
            }
        }
    }

    /** Builds the observer creation dialog of [communityId] and returns its opening, which starts with an empty form. */
    private fun observerDialog(communityId: CommunityId, onCreated: (ObserverVo) -> Unit): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageAdministrator
        val draft = Binder<ObserverDraft>()
        val creation = dialog(
            title = texts.communityObserverDialogTitle(),
            subtitle = texts.communityObserverDialogSubtitle(),
        ) {
            row {
                textInput(texts.communityObserverName(), labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules(texts.communityObserverNameRequired())
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                lookupMany(
                    texts.communityObserverContests(),
                    labelSize = 6,
                    size = 18,
                    fetch = { query, request ->
                        val contests =
                            administratorService.viewCommunityContests(communityId, request.toPagination(), query.ifEmpty { null })
                        Page(contests.content, contests.totalElements.toInt())
                    },
                    display = ContestVo::name,
                    columns = {
                        codeColumn(texts.id(), size = 6) { contest -> contest.id.value.toString() }
                        textColumn(texts.name(), size = 16) { contest -> contest.name }
                    },
                    hint = texts.communityObserverContestsHint(),
                    dialogSize = DialogSize.L,
                ) {
                    draft.forField(this)
                        .asRequired(texts.communityObserverContestsRequired())
                        .bind({ values -> values.contests }, { values, contests -> values.contests = contests })
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.create()) {
                    onClick {
                        val values = ObserverDraft()
                        if (draft.writeBeanIfValid(values)) {
                            val contestIds = values.contests.map { contest -> contest.id }.toSet()
                            val observer = administratorService.createObserver(communityId, values.name, contestIds)
                            toast(FeedbackKind.Success, currentLocalization().pageAdministrator.communityObserverCreated())
                            dialog.close()
                            onCreated(observer)
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(ObserverDraft())
            creation.open()
        }
    }

    private fun roleOf(kind: CommunityInvite.Kind): String = currentLocalization().pageCabinet.roleName(
        role = when (kind) {
            CommunityInvite.Kind.Manager -> RoleNameRole.MANAGER
            CommunityInvite.Kind.Developer -> RoleNameRole.DEVELOPER
        },
    )

    /** Values of the observer creation form. */
    private class ObserverDraft {
        var name: String = ""
        var contests: Set<ContestVo> = emptySet()
    }
}
