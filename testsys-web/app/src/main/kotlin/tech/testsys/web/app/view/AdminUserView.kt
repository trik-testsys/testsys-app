package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteAlias
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.web.app.service.administrator.AdminUserVo
import tech.testsys.web.app.service.administrator.AdministratorRoleVo
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.app.service.administrator.CommunityVo
import tech.testsys.web.app.service.administrator.DeveloperRoleVo
import tech.testsys.web.app.service.administrator.JudgeRoleVo
import tech.testsys.web.app.service.administrator.ManagerRoleVo
import tech.testsys.web.app.service.administrator.MultipleRoleUserVo
import tech.testsys.web.app.service.administrator.ObserverVo
import tech.testsys.web.app.service.administrator.RoleVo
import tech.testsys.web.app.service.administrator.StudentRoleVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts
import java.time.Instant

/** Labels of the roles an administrator grants. */
private val GRANTED_ROLE_LABELS: Map<CommunityRole, String> = mapOf(
    CommunityRole.Developer to "Разработчик",
    CommunityRole.Manager to "Организатор",
    CommunityRole.Student to "Ученик",
)

/**
 * User available to an Administrator (testsys.web.page.admin.user): the common data of the user, the roles in the
 * communities of the administrator and granting a role in one of them. Observers have their own address.
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/users/:userId([0-9]+)")
@RouteAlias("admin/observers/:observerId([0-9]+)")
@PageTitle("Пользователь")
@RolesAllowed("MULTIPLE_ROLE")
class AdminUserView(texts: UiTexts, private val headers: CabinetHeaders, private val administratorService: AdministratorService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val parameters = event.routeParameters
        val userId: UserId = parameters.getLong(USER_ID_PARAMETER).map<UserId>(::MultipleRoleUserId)
            .orElseGet { SingleRoleUserId(parameters.getLong(OBSERVER_ID_PARAMETER).orElseThrow()) }
        show(userId)
    }

    private fun show(userId: UserId) {
        val (user, lastLogin) = administratorService.viewUser(userId)
        val communities = administratorService.viewCommunities().map { (community, _) -> community }
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(user.name) {
                crumb("Кабинет Администратора", AdminView::class.java)
                crumb("Пользователи", AdminView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "users"))
            }
            detailsBlock(user, lastLogin)
            rolesBlock(user, communities)
        }
    }

    @RawAccessTokenDependency(reason = "Shows the stored access code of an observer as the issued one.")
    private fun PageScope.detailsBlock(user: AdminUserVo, lastLogin: Instant?) {
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 3, size = 6) { value = user.id.value.toString() }
                    textInput("Псевдоним", labelSize = 3, size = 8) { value = user.name }
                }
                row { dateTimeInput("Последний вход", labelSize = 3, size = 6) { value = lastLogin?.toServerDateTime() } }
                if (user is ObserverVo) {
                    row {
                        codeInput("Код-доступа", labelSize = 3, size = 6) {
                            value = user.accessTokenHash.value
                            isObscured = true
                        }
                    }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.rolesBlock(user: AdminUserVo, communities: List<CommunityVo>) {
        val communitiesById = communities.associateBy { community -> community.id }
        val memberships = when (user) {
            is MultipleRoleUserVo -> user.roles.flatMap { role ->
                role.memberOf.mapNotNull { communityId -> communitiesById[communityId]?.let { community -> community to labelOf(role) } }
            }
            is ObserverVo -> listOfNotNull(communitiesById[user.community]?.let { community -> community to "Наблюдатель" })
        }
        row {
            block(title = "Роли в сообществах") {
                table(
                    key = { (community, role): Pair<CommunityVo, String> -> community.id to role },
                    fetch = { request -> pageOf(memberships, request) },
                ) {
                    textColumn("Сообщество", size = 12) { (community, _) -> community.name }
                    textColumn("Роль") { (_, role) -> role }
                    empty("Ролей в ваших сообществах нет")
                    onRowClick { (community, _) -> openAdminCommunity(community.id) }
                }
                if (user is MultipleRoleUserVo) {
                    val granting = grantDialog(user, communities)
                    actions { action("Включить в сообщество") { onClick { granting() } } }
                }
            }
        }
    }

    /** Builds the role granting dialog of [user] and returns its opening, which starts with an empty form. */
    private fun grantDialog(user: MultipleRoleUserVo, communities: List<CommunityVo>): () -> Unit {
        val draft = Binder<GrantDraft>()
        val granting = dialog(title = "Включение в сообщество") {
            row {
                select("Сообщество", labelSize = 6, size = 18, items = communities, itemLabel = CommunityVo::name) {
                    draft.forField(this)
                        .asRequired("Выберите сообщество")
                        .bind({ values -> values.community }, { values, community -> values.community = community })
                }
            }
            row {
                val roles = GRANTED_ROLE_LABELS.keys.toList()
                select("Роль", labelSize = 6, size = 18, items = roles, itemLabel = GRANTED_ROLE_LABELS::getValue) {
                    draft.forField(this)
                        .asRequired("Выберите роль")
                        .bind({ values -> values.role }, { values, role -> values.role = role })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Включить") {
                    onClick {
                        val values = GrantDraft()
                        if (draft.writeBeanIfValid(values)) {
                            administratorService.grantRole(user.id, checkNotNull(values.community).id, checkNotNull(values.role))
                            dialog.close()
                            show(user.id)
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(GrantDraft())
            granting.open()
        }
    }

    private fun labelOf(role: RoleVo): String = when (role) {
        is AdministratorRoleVo -> "Администратор"
        is DeveloperRoleVo -> "Разработчик"
        is ManagerRoleVo -> "Организатор"
        is JudgeRoleVo -> "Судья"
        is StudentRoleVo -> "Ученик"
    }

    /** Values of the role granting form. */
    private class GrantDraft {
        var community: CommunityVo? = null
        var role: CommunityRole? = null
    }
}
