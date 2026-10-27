package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.texts.currentLocalization

/** Columns of the roles block; joining a community takes the rest of the row beside it. */
private const val ROLES_COLUMNS = 12

/**
 * Main page of the Cabinet of a user with non-fixed roles (testsys.web.page.multi.main): the roles of the user with links
 * to their pages, the communities of the user with the roles in each of them and joining a community by an invite code.
 *
 * @since %CURRENT_VERSION%
 */
@Route("home")
@RolesAllowed("MULTIPLE_ROLE")
class MultiMainView(private val headers: CabinetHeaders, private val multipleRoleUserService: MultipleRoleUserService) :
    TestSysView(), HasDynamicTitle {
    init {
        show()
    }

    override fun getPageTitle(): String = currentLocalization().pageHome.title()

    private fun show() {
        val profile = multipleRoleUserService.viewProfile().roles
        val texts = currentLocalization().pageHome
        page(headers.cabinet(active = CabinetHeaders.MAIN_SECTION)) {
            head(texts.title())
            guide(texts.guide())
            row {
                block(size = ROLES_COLUMNS, title = texts.rolesTitle()) {
                    guide(texts.rolesGuide())
                    table(
                        key = { (role, _): Pair<RoleVo, List<CommunityVo>> -> labelOf(role) },
                        fetch = { request -> pageOf(profile, request) },
                    ) {
                        column(texts.rolesRole(), size = 14) { (role, _) -> tag(labelOf(role)) }
                        numberColumn(texts.rolesCommunities()) { (_, communities) -> communities.size }
                        empty(texts.rolesEmpty(), texts.joinHint())
                        onRowClick(isNavigation = true) { (role, _) -> UI.getCurrent().navigate(rolePageOf(role)) }
                    }
                }
                block(title = texts.joinTitle()) {
                    guide(texts.joinGuide())
                    lateinit var inviteCode: ValueInput<String>
                    row { inviteCode = codeInput(texts.joinInviteCode(), labelSize = 6, size = 6) }
                    footer {
                        mainAction(texts.joinButton()) {
                            clickOnEnter()
                            onClick { joinCommunity(inviteCode.value) }
                        }
                    }
                }
            }
            row {
                block(title = texts.communitiesTitle()) {
                    guide(texts.communitiesGuide())
                    val communities = communitiesOf(profile)
                    table(
                        key = { (community, _): Pair<CommunityVo, List<RoleVo>> -> community.id },
                        fetch = { request -> pageOf(communities, request) },
                    ) {
                        codeColumn(texts.communitiesId(), size = 4) { (community, _) -> community.id.value.toString() }
                        textColumn(texts.communitiesName(), size = 8) { (community, _) -> community.name }
                        textColumn(texts.communitiesDescription(), size = 7) { (community, _) -> community.description }
                        column(texts.communitiesRoles()) { (_, roles) -> roles.forEach { role -> tag(labelOf(role)) } }
                        empty(texts.communitiesEmpty(), texts.joinHint())
                    }
                }
            }
        }
    }

    /** Joins the community by [inviteCode] and rebuilds the page. */
    private fun joinCommunity(inviteCode: String) {
        val texts = currentLocalization().pageHome
        try {
            multipleRoleUserService.joinCommunity(inviteCode)
        } catch (exception: OperationException) {
            when (val error = exception.error) {
                is JoinCommunityError -> when (error) {
                    is CommunityInviteCodeNotValidError -> toast(FeedbackKind.Error, texts.inviteCodeInvalid())
                    is CommunityInviteCodeExpiredError -> toast(FeedbackKind.Error, texts.inviteCodeExpired())
                }
                else -> throw exception
            }
            return
        }
        toast(FeedbackKind.Success, texts.joined())
        show()
    }

    /** Lists each community of [profile] once, in ascending id order, with the roles of the user in it. */
    private fun communitiesOf(profile: List<Pair<RoleVo, List<CommunityVo>>>): List<Pair<CommunityVo, List<RoleVo>>> = profile
        .flatMap { (role, communities) -> communities.map { community -> community to role } }
        .groupBy({ (community, _) -> community.id }, { pair -> pair })
        .values
        .map { pairs -> pairs.first().first to pairs.map { (_, role) -> role } }
        .sortedBy { (community, _) -> community.id.value }
}
