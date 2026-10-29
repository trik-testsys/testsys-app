package tech.testsys.web.app.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.security.AuthenticationContext
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.infra.localization.PageCabinet
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.service.AdministratorRoleVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.DeveloperRoleVo
import tech.testsys.web.app.service.JudgeRoleVo
import tech.testsys.web.app.service.ManagerRoleVo
import tech.testsys.web.app.service.RoleVo
import tech.testsys.web.app.service.StudentRoleVo
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.navigation.header.HeaderDestination
import tech.testsys.web.components.navigation.header.HeaderMegaColumn
import tech.testsys.web.components.navigation.header.HeaderMegaLink
import tech.testsys.web.components.navigation.header.HeaderMegaMenu
import tech.testsys.web.components.navigation.header.HeaderUser
import tech.testsys.web.components.navigation.header.HeaderUserMenu
import tech.testsys.web.components.navigation.header.HeaderUserMenuItem
import tech.testsys.web.components.navigation.header.MegaMenuItem
import tech.testsys.web.components.navigation.header.NavItem
import tech.testsys.web.components.texts.currentLocalization

/**
 * Returns the start page of a user of [kind]: the main page for a user with non-fixed roles, the page of the fixed role
 * for the others.
 *
 * @since %CURRENT_VERSION%
 */
fun startPageOf(kind: UserKind): Class<out Component> = when (kind) {
    UserKind.MULTIPLE_ROLE -> MultiMainView::class.java
    UserKind.PARTICIPANT -> ParticipantView::class.java
    UserKind.OBSERVER -> ObserverView::class.java
    UserKind.SUPERVISOR -> SupervisorView::class.java
}

/**
 * Returns the label of [role] for pages in the region of the current UI.
 *
 * @since %CURRENT_VERSION%
 */
fun labelOf(role: RoleVo): String = currentLocalization().pageCabinet.roleName(
    role = when (role) {
        is AdministratorRoleVo -> RoleNameRole.ADMINISTRATOR
        is DeveloperRoleVo -> RoleNameRole.DEVELOPER
        is ManagerRoleVo -> RoleNameRole.MANAGER
        is JudgeRoleVo -> RoleNameRole.JUDGE
        is StudentRoleVo -> RoleNameRole.STUDENT
    },
)

/**
 * Returns the page of [role].
 *
 * @since %CURRENT_VERSION%
 */
fun rolePageOf(role: RoleVo): Class<out Component> = when (role) {
    is AdministratorRoleVo -> AdminView::class.java
    is DeveloperRoleVo -> DeveloperView::class.java
    is ManagerRoleVo -> ManagerView::class.java
    is JudgeRoleVo -> JudgeView::class.java
    is StudentRoleVo -> StudentView::class.java
}

/**
 * Builds the Cabinet header of the signed-in user (testsys.web.component.header).
 * Its companion holds the keys of the sections «Главная», active on the start pages, and «Меню», active on the role pages.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class CabinetHeaders(private val currentUser: CurrentUser, private val authenticationContext: AuthenticationContext) {
    /**
     * Returns the Cabinet header of the signed-in user with the [active] section: the start page, the menu of the roles
     * with search and the nickname menu with the profile, unless the role is fixed, the return of a supervisor signed in
     * as the user, and signing out.
     *
     * @since %CURRENT_VERSION%
     */
    fun cabinet(active: String? = null): CabinetHeader {
        val user = currentUser.user()
        val texts = currentLocalization().pageCabinet
        val signOut = HeaderUserMenuItem(
            label = texts.signOut(),
            destination = HeaderDestination.Action { authenticationContext.logout() },
            isDestructive = true,
        )

        val isImpersonated = CabinetSignIn.principal()?.impersonatedBy != null
        val returnToSupervisor = HeaderUserMenuItem(
            label = texts.impersonationReturn(),
            destination = HeaderDestination.Action { returnToSupervisor() },
        ).takeIf { isImpersonated }
        val profile = HeaderUserMenuItem(texts.profile(), HeaderDestination.Route(ProfileView::class.java))
            .takeIf { user is MultipleRoleUser }
        val userMenu = listOfNotNull(profile, returnToSupervisor, signOut)
        val name = if (isImpersonated) texts.impersonationName(name = nameOf(user)) else nameOf(user)

        return CabinetHeader(
            items = listOf(
                NavItem(key = MAIN_SECTION, label = texts.home(), target = startPageOf(CabinetPrincipal.of(user).kind)),
                MegaMenuItem(key = MENU_SECTION, label = texts.menu(), menu = HeaderMegaMenu(columnsOf(user, texts))),
            ),
            active = active,
            user = HeaderUser(name = name, menu = HeaderUserMenu(userMenu)),
            menuSearchKey = MENU_SECTION,
        )
    }

    /** Signs the supervisor back in and opens their details of the user they leave (testsys.user.single.supervisor.impersonate). */
    private fun returnToSupervisor() {
        val userId = CabinetSignIn.returnToSupervisor() ?: return
        UI.getCurrent().navigate(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, userId.toString()))
    }

    private fun columnsOf(user: User<*>, texts: PageCabinet): List<HeaderMegaColumn> = when (user) {
        is MultipleRoleUser -> user.data.roles.map { role -> columnOf(role, texts) }.distinct().sortedBy { column -> column.title }
        is Participant -> listOf(
            roleColumn(
                title = texts.roleName(role = RoleNameRole.PARTICIPANT),
                page = ParticipantView::class.java,
                sections = listOf(texts.sectionContests() to "contests"),
            ),
        )
        is Observer -> listOf(
            roleColumn(
                title = texts.roleName(role = RoleNameRole.OBSERVER),
                page = ObserverView::class.java,
                sections = listOf(texts.sectionContests() to "contests"),
            ),
        )
        is Supervisor -> listOf(
            roleColumn(
                title = texts.roleName(role = RoleNameRole.SUPERVISOR),
                page = SupervisorView::class.java,
                sections = listOf(texts.sectionUsers() to "users", texts.sectionCommunities() to "communities"),
            ),
        )
    }

    private fun columnOf(role: CompatibleUserRole, texts: PageCabinet): HeaderMegaColumn = when (role) {
        is Developer -> roleColumn(
            title = texts.roleName(role = RoleNameRole.DEVELOPER),
            page = DeveloperView::class.java,
            sections = listOf(texts.sectionTasks() to "tasks", texts.sectionContests() to "contests"),
        )
        is Manager -> roleColumn(
            title = texts.roleName(role = RoleNameRole.MANAGER),
            page = ManagerView::class.java,
            sections = listOf(texts.sectionClasses() to "classes", texts.sectionCompetitions() to "competitions"),
        )
        is Administrator -> roleColumn(
            title = texts.roleName(role = RoleNameRole.ADMINISTRATOR),
            page = AdminView::class.java,
            sections = listOf(texts.sectionCommunities() to "communities", texts.sectionUsers() to "users"),
        )
        is Judge -> roleColumn(
            title = texts.roleName(role = RoleNameRole.JUDGE),
            page = JudgeView::class.java,
            sections = listOf(texts.sectionSubmissions() to "submissions"),
        )
        is Student -> roleColumn(
            title = texts.roleName(role = RoleNameRole.STUDENT),
            page = StudentView::class.java,
            sections = listOf(texts.sectionClasses() to "classes"),
        )
    }

    /**
     * Menu column of one role: the heading opens the page of the role, and each of its [sections], a label with the
     * value of the route parameter [SECTION_PARAMETER], opens that section of the page. Each link has its own location,
     * so the menu marks only the one that was opened.
     */
    private fun roleColumn(title: String, page: Class<out Component>, sections: List<Pair<String, String>>): HeaderMegaColumn {
        val links = sections.map { (label, section) ->
            val destination = HeaderDestination.Route(page, RouteParameters(SECTION_PARAMETER, section))
            HeaderMegaLink(label = label, destination = destination)
        }
        return HeaderMegaColumn(title = title, links = links, destination = HeaderDestination.Route(page))
    }

    private fun nameOf(user: User<*>): String = when (user) {
        is MultipleRoleUser -> user.data.name
        is Participant -> user.data.name
        is Observer -> user.data.name
        is Supervisor -> user.data.name
    }

    companion object {
        const val MAIN_SECTION: String = "main"
        const val MENU_SECTION: String = "menu"
        private const val SECTION_PARAMETER: String = "section"
    }
}
