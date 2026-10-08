package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.operation.error.CommunityInviteCodeExpiredError
import tech.testsys.operation.error.CommunityInviteCodeNotValidError
import tech.testsys.operation.error.JoinCommunityError
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.RoleVo
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.UiTexts

/** Columns of the roles block; joining a community takes the rest of the row beside it. */
private const val ROLES_COLUMNS = 12

/**
 * Main page of the Cabinet of a user with non-fixed roles (testsys.web.page.multi.main): the roles of the user with links
 * to their pages, the communities of the user with the roles in each of them and joining a community by an invite code.
 * Right after registration it shows the access code once.
 *
 * @since %CURRENT_VERSION%
 */
@Route("home")
@PageTitle("Главная")
@RolesAllowed("MULTIPLE_ROLE")
class MultiMainView(texts: UiTexts, private val headers: CabinetHeaders, private val multipleRoleUserService: MultipleRoleUserService) :
    TestSysView(texts) {
    init {
        show(accessToken = null)
    }

    /**
     * Rebuilds the page with [accessToken] of the user who has just registered and a request to save it.
     *
     * @since %CURRENT_VERSION%
     */
    fun showAccessToken(accessToken: String) {
        show(accessToken)
    }

    private fun show(accessToken: String?) {
        val profile = multipleRoleUserService.viewProfile().roles
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            head("Главная")
            accessToken?.let { issued -> accessTokenBlock(issued) }
            row {
                block(size = ROLES_COLUMNS, title = "Роли") {
                    table(
                        key = { (role, _): Pair<RoleVo, List<CommunityVo>> -> labelOf(role) },
                        fetch = { request -> pageOf(profile, request) },
                    ) {
                        textColumn("Роль", size = 14) { (role, _) -> labelOf(role) }
                        numberColumn("Сообществ") { (_, communities) -> communities.size }
                        empty("Ролей пока нет", "Присоединитесь к сообществу по Коду-приглашению.")
                        onRowClick(isNavigation = true) { (role, _) -> UI.getCurrent().navigate(rolePageOf(role)) }
                    }
                }
                block(title = "Присоединиться к сообществу") {
                    lateinit var inviteCode: ValueInput<String>
                    row { inviteCode = codeInput("Код-приглашение", labelSize = 6, size = 6) }
                    footer {
                        mainAction("Присоединиться") {
                            clickOnEnter()
                            onClick { joinCommunity(inviteCode.value, accessToken) }
                        }
                    }
                }
            }
            row {
                block(title = "Сообщества") {
                    val communities = communitiesOf(profile)
                    table(
                        key = { (community, _): Pair<CommunityVo, List<RoleVo>> -> community.id },
                        fetch = { request -> pageOf(communities, request) },
                    ) {
                        codeColumn("ID", size = 4) { (community, _) -> community.id.value.toString() }
                        textColumn("Название", size = 8) { (community, _) -> community.name }
                        textColumn("Описание", size = 7) { (community, _) -> community.description }
                        textColumn("Роли") { (_, roles) -> roles.joinToString(", ") { role -> labelOf(role) } }
                        empty("Сообществ пока нет", "Присоединитесь к сообществу по Коду-приглашению.")
                    }
                }
            }
        }
    }

    private fun PageScope.accessTokenBlock(accessToken: String) {
        row {
            block(title = "Код-доступа") {
                row {
                    alert(
                        kind = FeedbackKind.Warning,
                        title = "Сохраните Код-доступа",
                        text = "По нему вы входите в Систему. Код также отправлен на почту.",
                    )
                }
                row {
                    codeInput("Код-доступа", labelSize = 6, size = 18) {
                        value = accessToken
                        isEditable = false
                        isObscured = true
                    }
                }
            }
        }
    }

    /** Joins the community by [inviteCode] and rebuilds the page, keeping the shown [accessToken]. */
    private fun joinCommunity(inviteCode: String, accessToken: String?) {
        try {
            multipleRoleUserService.joinCommunity(inviteCode)
        } catch (exception: OperationException) {
            when (val error = exception.error) {
                is JoinCommunityError -> when (error) {
                    is CommunityInviteCodeNotValidError -> toast(FeedbackKind.Error, "Код-приглашение недействителен")
                    is CommunityInviteCodeExpiredError -> toast(FeedbackKind.Error, "Срок Кода-приглашения истёк")
                }
                else -> throw exception
            }
            return
        }
        toast(FeedbackKind.Success, "Вы присоединились к сообществу")
        show(accessToken)
    }

    /** Lists each community of [profile] once, in ascending id order, with the roles of the user in it. */
    private fun communitiesOf(profile: List<Pair<RoleVo, List<CommunityVo>>>): List<Pair<CommunityVo, List<RoleVo>>> = profile
        .flatMap { (role, communities) -> communities.map { community -> community to role } }
        .groupBy({ (community, _) -> community.id }, { pair -> pair })
        .values
        .map { pairs -> pairs.first().first to pairs.map { (_, role) -> role } }
        .sortedBy { (community, _) -> community.id.value }
}
