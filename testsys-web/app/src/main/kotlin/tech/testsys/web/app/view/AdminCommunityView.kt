package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.user.RawAccessTokenDependency
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
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.lookupMany
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts

/** Columns of the community details block; the invite codes take the rest of the row beside it. */
private const val DETAILS_COLUMNS = 12

/**
 * Community of an Administrator (testsys.web.page.admin.community): its name and description with their editing, its
 * invite codes, its users and creation of its observers; right after creating an observer it shows its access code once.
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/communities/:communityId([0-9]+)")
@PageTitle("Сообщество")
@RolesAllowed("MULTIPLE_ROLE")
class AdminCommunityView(texts: UiTexts, private val headers: CabinetHeaders, private val administratorService: AdministratorService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val communityId = CommunityId(event.routeParameters.getLong(COMMUNITY_ID_PARAMETER).orElseThrow())
        // Checks the role, the community and its owner before the tables load, so that a refusal opens its error screen.
        administratorService.viewCommunityInvites(communityId)
        val communities = administratorService.viewCommunities().map { (community, _) -> community }
        show(communities.first { community -> community.id == communityId }, communities, observer = null)
    }

    private fun show(community: CommunityVo, communities: List<CommunityVo>, observer: ObserverVo?) {
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(community.name) {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Администратора", AdminView::class.java)
                crumb("Сообщества", AdminView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "communities"))
            }
            row {
                detailsBlock(community) { edited -> show(edited, communities, observer) }
                invitesBlock(community.id)
            }
            observer?.let { created -> createdObserverBlock(created) }
            row {
                block(title = "Пользователи сообщества") {
                    adminUsersTable(administratorService, communities, community.id)
                    val creation = observerDialog(community.id) { created -> show(community, communities, created) }
                    actions { action("Создать наблюдателя") { onClick { creation() } } }
                }
            }
        }
    }

    /** Shows the name and description of [community] for viewing; a saved change rebuilds the page through [onEdited]. */
    private fun PageRowScope.detailsBlock(community: CommunityVo, onEdited: (CommunityVo) -> Unit) {
        val draft = Binder<CommunityDraft>()
        block(size = DETAILS_COLUMNS, title = "Сведения") {
            editing(
                onSave = {
                    val values = CommunityDraft()
                    val isValid = draft.writeBeanIfValid(values)
                    if (isValid) {
                        onEdited(
                            administratorService.editCommunity(
                                communityId = community.id,
                                communityName = values.name,
                                description = values.description,
                            ),
                        )
                    }
                    isValid
                },
                onCancel = { draft.readBean(CommunityDraft(community.name, community.description)) },
            )
            row {
                textInput("Название", labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules("Укажите название")
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                textArea("Описание", labelSize = 6, size = 18) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
        }

        draft.readBean(CommunityDraft(community.name, community.description))
    }

    @RawAccessTokenDependency(reason = "Shows the stored access code of the created observer as the issued one.")
    private fun PageScope.createdObserverBlock(observer: ObserverVo) {
        row {
            block(title = "Наблюдатель «${observer.name}» создан") {
                row { alert(kind = FeedbackKind.Warning, title = "Сохраните Код-доступа", text = "Наблюдатель входит по нему в Систему.") }
                row {
                    codeInput("Код-доступа", labelSize = 6, size = 18) {
                        value = observer.accessTokenHash.value
                        isEditable = false
                        isObscured = true
                    }
                }
                footer { linkAction("Открыть наблюдателя") { onClick { openAdminUser(observer) } } }
            }
        }
    }

    @RawInviteCodeDependency(reason = "Shows the stored invite codes as the issued ones.")
    private fun PageRowScope.invitesBlock(communityId: CommunityId) {
        block(title = "Коды-приглашения") {
            // The menu of a row refreshes the table that contains it.
            lateinit var rows: TableHandle<CommunityInviteVo>
            rows = table(
                key = { invite: CommunityInviteVo -> invite.kind },
                fetch = { request -> pageOf(administratorService.viewCommunityInvites(communityId), request) },
            ) {
                textColumn("Роль", size = 6) { invite -> roleOf(invite.kind) }
                codeColumn("Код-приглашение", size = 7, isObscured = true) { invite -> invite.codeHash.value }
                dateTimeColumn("Действует до") { invite -> invite.expiresAt.toServerDateTime() }
                menuColumn(ariaLabel = { invite -> "Действия с кодом-приглашением для роли ${roleOf(invite.kind)}" }, size = 2) { invite ->
                    item("Заменить") {
                        confirm(
                            title = "Заменить код-приглашение?",
                            text = "Прежний Код-приглашение перестанет действовать.",
                            action = "Заменить",
                            isDanger = true,
                        ) {
                            administratorService.createCommunityInvite(communityId, invite.kind)
                            rows.refresh()
                        }
                    }
                    item("Продлить") {
                        administratorService.extendCommunityInvite(communityId, invite.kind)
                        rows.refresh()
                    }
                }
            }
        }
    }

    /** Builds the observer creation dialog of [communityId] and returns its opening, which starts with an empty form. */
    private fun observerDialog(communityId: CommunityId, onCreated: (ObserverVo) -> Unit): () -> Unit {
        val draft = Binder<ObserverDraft>()
        val creation = dialog(title = "Новый наблюдатель", subtitle = "Наблюдатель видит результаты выбранных туров") {
            row {
                textInput("Имя", labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules("Укажите имя")
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                lookupMany(
                    "Туры",
                    labelSize = 6,
                    size = 18,
                    fetch = { query, request ->
                        val contests =
                            administratorService.viewCommunityContests(communityId, request.toPagination(), query.ifEmpty { null })
                        Page(contests.content, contests.totalElements.toInt())
                    },
                    display = ContestVo::name,
                    columns = {
                        codeColumn("ID", size = 6) { contest -> contest.id.value.toString() }
                        textColumn("Название", size = 16) { contest -> contest.name }
                    },
                    hint = "Туры, открытые сообществу",
                ) {
                    draft.forField(this)
                        .asRequired("Выберите туры")
                        .bind({ values -> values.contests }, { values, contests -> values.contests = contests })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Создать") {
                    onClick {
                        val values = ObserverDraft()
                        if (draft.writeBeanIfValid(values)) {
                            val contestIds = values.contests.map { contest -> contest.id }.toSet()
                            val observer = administratorService.createObserver(communityId, values.name, contestIds)
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

    private fun roleOf(kind: CommunityInvite.Kind): String = when (kind) {
        CommunityInvite.Kind.Manager -> "Организатор"
        CommunityInvite.Kind.Developer -> "Разработчик"
    }

    /** Values of the observer creation form. */
    private class ObserverDraft {
        var name: String = ""
        var contests: Set<ContestVo> = emptySet()
    }
}
