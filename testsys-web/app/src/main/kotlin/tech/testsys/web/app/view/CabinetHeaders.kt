package tech.testsys.web.app.view

import com.vaadin.flow.component.Component
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
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.service.CurrentUser
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
 * Builds the headers of the pages: the guest header and the Cabinet header of the signed-in user (testsys.web.component.header).
 * Its companion holds the keys of the sections «Главная», active on the start pages, and «Меню», active on the role pages.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class CabinetHeaders(private val currentUser: CurrentUser, private val authenticationContext: AuthenticationContext) {
    /**
     * Returns the header of a guest with the sign-in link if [hasSignIn].
     *
     * @since %CURRENT_VERSION%
     */
    fun guest(hasSignIn: Boolean = true): CabinetHeader = CabinetHeader(signIn = AuthenticationView::class.java.takeIf { hasSignIn })

    /**
     * Returns the Cabinet header of the signed-in user with the [active] section: the start page, the menu of the roles
     * with search and the nickname menu with the profile, unless the role is fixed, and signing out.
     *
     * @since %CURRENT_VERSION%
     */
    fun cabinet(active: String? = null): CabinetHeader {
        val user = currentUser.user()
        val signOut = HeaderUserMenuItem(
            label = "Выйти",
            destination = HeaderDestination.Action { authenticationContext.logout() },
            isDestructive = true,
        )

        val userMenu = if (user is MultipleRoleUser) {
            listOf(HeaderUserMenuItem("Профиль", HeaderDestination.Route(ProfileView::class.java)), signOut)
        } else {
            listOf(signOut)
        }

        return CabinetHeader(
            items = listOf(
                NavItem(key = MAIN_SECTION, label = "Главная", target = startPageOf(CabinetPrincipal.of(user).kind)),
                MegaMenuItem(key = MENU_SECTION, label = "Меню", menu = HeaderMegaMenu(columnsOf(user))),
            ),
            active = active,
            user = HeaderUser(name = nameOf(user), menu = HeaderUserMenu(userMenu)),
            menuSearchKey = MENU_SECTION,
        )
    }

    private fun columnsOf(user: User<*>): List<HeaderMegaColumn> = when (user) {
        is MultipleRoleUser -> user.data.roles.map { role -> columnOf(role) }.distinct().sortedBy { column -> column.title }
        is Participant -> listOf(
            roleColumn(
                title = "Участник",
                page = ParticipantView::class.java,
                sections = listOf("Туры" to "contests"),
            ),
        )
        is Observer -> listOf(
            roleColumn(
                title = "Наблюдатель",
                page = ObserverView::class.java,
                sections = listOf("Туры" to "contests"),
            ),
        )
        is Supervisor -> listOf(
            roleColumn(
                title = "Супервайзер",
                page = SupervisorView::class.java,
                sections = listOf("Пользователи" to "users", "Сообщества" to "communities"),
            ),
        )
    }

    private fun columnOf(role: CompatibleUserRole): HeaderMegaColumn = when (role) {
        is Developer -> roleColumn(
            title = "Разработчик",
            page = DeveloperView::class.java,
            sections = listOf("Задачи" to "tasks", "Туры" to "contests"),
        )
        is Manager -> roleColumn(
            title = "Организатор",
            page = ManagerView::class.java,
            sections = listOf("Классы" to "classes", "Соревнования" to "competitions"),
        )
        is Administrator -> roleColumn(
            title = "Администратор",
            page = AdminView::class.java,
            sections = listOf("Сообщества" to "communities", "Пользователи" to "users"),
        )
        is Judge -> roleColumn(
            title = "Судья",
            page = JudgeView::class.java,
            sections = listOf("Посылки" to "submissions"),
        )
        is Student -> roleColumn(
            title = "Ученик",
            page = StudentView::class.java,
            sections = listOf("Классы" to "classes"),
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
