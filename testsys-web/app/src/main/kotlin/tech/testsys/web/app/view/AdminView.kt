package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.app.service.administrator.CommunityVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts

private const val COMMUNITIES_SECTION = "communities"

private const val USERS_SECTION = "users"

/**
 * Cabinet of an Administrator (testsys.web.page.admin): the tabs «Сообщества» and «Пользователи» are the sections of the
 * route; without a section the page opens «Сообщества».
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/:section?(communities|users)")
@PageTitle("Кабинет Администратора")
@RolesAllowed("MULTIPLE_ROLE")
class AdminView(texts: UiTexts, private val headers: CabinetHeaders, private val administratorService: AdministratorService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val section = event.routeParameters.get(ADMIN_SECTION_PARAMETER).orElse(null)
        if (section == null) {
            event.forwardTo(AdminView::class.java, sectionParameters(COMMUNITIES_SECTION))
            return
        }

        // Also checks the administrator role before the tables load, so that its absence opens the forbidden screen.
        val communities = administratorService.viewCommunities().map { (community, _) -> community }
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Кабинет Администратора") {
                tabs(matchRouteParameters = true) {
                    tab("Сообщества", AdminView::class.java, sectionParameters(COMMUNITIES_SECTION))
                    tab("Пользователи", AdminView::class.java, sectionParameters(USERS_SECTION))
                }
            }
            if (section == USERS_SECTION) {
                row { block(title = "Пользователи") { adminUsersTable(administratorService, communities, community = null) } }
            } else {
                communitiesBlock()
            }
        }
    }

    private fun PageScope.communitiesBlock() {
        row {
            block(title = "Сообщества") {
                val rows = table(
                    key = { (community, _): Pair<CommunityVo, Long> -> community.id },
                    fetch = { request -> pageOf(administratorService.viewCommunities(), request) },
                ) {
                    codeColumn("ID", size = 4) { (community, _) -> community.id.value.toString() }
                    textColumn("Название", size = 7) { (community, _) -> community.name }
                    textColumn("Описание", size = 9) { (community, _) -> community.description }
                    numberColumn("Пользователей") { (_, users) -> users }
                    empty("Сообществ пока нет", "Создайте сообщество и пригласите в него Пользователей.")
                    onRowClick { (community, _) -> openAdminCommunity(community.id) }
                }

                val draft = Binder<CommunityDraft>()
                val creation = dialog(title = "Новое сообщество") {
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
                    footer { dialog ->
                        action("Отменить") { onClick { dialog.close() } }
                        mainAction("Создать") {
                            onClick {
                                val created = CommunityDraft()
                                if (draft.writeBeanIfValid(created)) {
                                    administratorService.createCommunity(communityName = created.name, description = created.description)
                                    dialog.close()
                                    rows.refresh()
                                }
                            }
                        }
                    }
                }
                actions {
                    action("Создать сообщество") {
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
