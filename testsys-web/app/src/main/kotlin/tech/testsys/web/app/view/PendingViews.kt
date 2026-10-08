package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.texts.UiTexts

/**
 * Cabinet page whose content is not implemented yet: the Cabinet header with the [active] section and an empty state
 * under [title]. The routes and access of the pages are final, so the header can lead to them; the optional route
 * parameter `section` selects the section of a role page that the menu leads to.
 *
 * @since %CURRENT_VERSION%
 */
abstract class PendingCabinetView(texts: UiTexts, headers: CabinetHeaders, title: String, active: String?) : TestSysView(texts) {
    init {
        page(headers.cabinet(active)) {
            row { block(title = title) { emptyState("Раздел пока не реализован") } }
        }
    }
}

/**
 * Cabinet of a Participant (testsys.web.page.participant), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("participant/:section?(contests)")
@PageTitle("Кабинет Участника")
@RolesAllowed("PARTICIPANT")
class ParticipantView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Участника", active = CabinetHeaders.MAIN_SECTION)

/**
 * Cabinet of an Observer (testsys.web.page.observer), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("observer/:section?(contests)")
@PageTitle("Кабинет Наблюдателя")
@RolesAllowed("OBSERVER")
class ObserverView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Наблюдателя", active = CabinetHeaders.MAIN_SECTION)

/**
 * Cabinet of a Supervisor (testsys.web.page.supervisor), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/:section?(users|communities)")
@PageTitle("Кабинет Супервайзера")
@RolesAllowed("SUPERVISOR")
class SupervisorView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Супервайзера", active = CabinetHeaders.MAIN_SECTION)

/**
 * Cabinet of a Developer (testsys.web.page.developer), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/:section?(tasks|contests)")
@PageTitle("Кабинет Разработчика")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Разработчика", active = CabinetHeaders.MENU_SECTION)

/**
 * Cabinet of a Manager (testsys.web.page.manager), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/:section?(classes|competitions)")
@PageTitle("Кабинет Организатора")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Организатора", active = CabinetHeaders.MENU_SECTION)

/**
 * Cabinet of an Administrator (testsys.web.page.admin), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/:section?(communities|users)")
@PageTitle("Кабинет Администратора")
@RolesAllowed("MULTIPLE_ROLE")
class AdminView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Администратора", active = CabinetHeaders.MENU_SECTION)

/**
 * Cabinet of a Judge (testsys.web.page.judge), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("judge/:section?(submissions)")
@PageTitle("Кабинет Судьи")
@RolesAllowed("MULTIPLE_ROLE")
class JudgeView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Судьи", active = CabinetHeaders.MENU_SECTION)

/**
 * Cabinet of a Student (testsys.web.page.student), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/:section?(classes)")
@PageTitle("Кабинет Ученика")
@RolesAllowed("MULTIPLE_ROLE")
class StudentView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Ученика", active = CabinetHeaders.MENU_SECTION)

/**
 * Profile of a user with non-fixed roles (testsys.web.page.multi.profile), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("profile")
@PageTitle("Профиль")
@RolesAllowed("MULTIPLE_ROLE")
class ProfileView(texts: UiTexts, headers: CabinetHeaders) : PendingCabinetView(texts, headers, title = "Профиль", active = null)
