package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.texts.UiTexts

/**
 * Cabinet page whose content is not implemented yet: the Cabinet header with the [active] section, the head [title] and
 * an empty state. A page opened from the main page of a user with non-fixed roles has [hasHomeCrumb] set, so that its
 * breadcrumbs start with «Главная». The routes and access of the pages are final, so the header can lead to them; the
 * optional route parameter `section` selects the section of a role page that the menu leads to.
 *
 * @since %CURRENT_VERSION%
 */
abstract class PendingCabinetView(texts: UiTexts, headers: CabinetHeaders, title: String, active: String?, hasHomeCrumb: Boolean) :
    TestSysView(texts) {
    init {
        page(headers.cabinet(active)) {
            head(title) { if (hasHomeCrumb) crumb("Главная", MultiMainView::class.java) }
            row { block { emptyState("Раздел пока не реализован") } }
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
    PendingCabinetView(texts, headers, title = "Кабинет Участника", active = CabinetHeaders.MAIN_SECTION, hasHomeCrumb = false)

/**
 * Cabinet of an Observer (testsys.web.page.observer), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("observer/:section?(contests)")
@PageTitle("Кабинет Наблюдателя")
@RolesAllowed("OBSERVER")
class ObserverView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Наблюдателя", active = CabinetHeaders.MAIN_SECTION, hasHomeCrumb = false)

/**
 * Cabinet of a Supervisor (testsys.web.page.supervisor), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/:section?(users|communities)")
@PageTitle("Кабинет Супервайзера")
@RolesAllowed("SUPERVISOR")
class SupervisorView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Супервайзера", active = CabinetHeaders.MAIN_SECTION, hasHomeCrumb = false)

/**
 * Cabinet of a Developer (testsys.web.page.developer), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/:section?(tasks|contests)")
@PageTitle("Кабинет Разработчика")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperView(texts: UiTexts, headers: CabinetHeaders) :
    PendingCabinetView(texts, headers, title = "Кабинет Разработчика", active = CabinetHeaders.MENU_SECTION, hasHomeCrumb = true)
