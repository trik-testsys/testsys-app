package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization

private const val COMMUNITIES_SECTION = "communities"

private const val USERS_SECTION = "users"

/**
 * Cabinet of an Administrator (testsys.web.page.admin): the tabs «Сообщества» and «Пользователи» are the sections of the
 * route; without a section the page opens «Сообщества».
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/:section?(communities|users)")
@RolesAllowed("MULTIPLE_ROLE")
class AdminView(private val headers: CabinetHeaders, private val administratorService: AdministratorService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageAdmin.title()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val section = event.routeParameters.get(ADMIN_SECTION_PARAMETER).orElse(null)
        if (section == null) {
            event.forwardTo(AdminView::class.java, sectionParameters(COMMUNITIES_SECTION))
            return
        }

        // Also checks the administrator role before the tables load, so that its absence opens the forbidden screen.
        val communities = administratorService.viewCommunities().map { (community, _) -> community }
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.title()) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                tabs(matchRouteParameters = true) {
                    tab(localization.pageCabinet.sectionCommunities(), AdminView::class.java, sectionParameters(COMMUNITIES_SECTION))
                    tab(localization.pageCabinet.sectionUsers(), AdminView::class.java, sectionParameters(USERS_SECTION))
                }
            }
            guide(texts.guide())
            if (section == USERS_SECTION) {
                row {
                    block(title = localization.pageCabinet.sectionUsers()) {
                        guide(texts.usersGuide())
                        adminUsersTable(administratorService, communities, community = null)
                    }
                }
            } else {
                communitiesBlock()
            }
        }
    }

    private fun PageScope.communitiesBlock() {
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        row {
            block(title = localization.pageCabinet.sectionCommunities()) {
                guide(texts.communitiesGuide())
                val rows = table(
                    key = { (community, _): Pair<CommunityVo, Long> -> community.id },
                    fetch = { request -> pageOf(administratorService.viewCommunities(), request) },
                ) {
                    codeColumn(texts.id(), size = 4) { (community, _) -> community.id.value.toString() }
                    textColumn(texts.name(), size = 7) { (community, _) -> community.name }
                    textColumn(texts.description(), size = 9) { (community, _) -> community.description }
                    numberColumn(texts.communitiesUsers()) { (_, users) -> users }
                    empty(texts.communitiesEmpty(), texts.communitiesEmptyHint())
                    onRowClick(isNavigation = true) { (community, _) -> openAdminCommunity(community.id) }
                }

                val draft = Binder<CommunityDraft>()
                val creation = dialog(title = texts.communitiesDialogTitle()) {
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
                    footer { dialog ->
                        action(localization.uiDialog.cancel()) { onClick { dialog.close() } }
                        mainAction(texts.create()) {
                            onClick {
                                val created = CommunityDraft()
                                if (draft.writeBeanIfValid(created)) {
                                    administratorService.createCommunity(communityName = created.name, description = created.description)
                                    toast(FeedbackKind.Success, currentLocalization().pageAdmin.communitiesCreated())
                                    dialog.close()
                                    rows.refresh()
                                }
                            }
                        }
                    }
                }
                actions {
                    action(texts.communitiesCreate()) {
                        onClick {
                            draft.readBean(CommunityDraft())
                            creation.open()
                        }
                    }
                }
            }
        }
    }

    private fun sectionParameters(section: String): RouteParameters = RouteParameters(ADMIN_SECTION_PARAMETER, section)
}
