package tech.testsys.web.app.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.navigation.tabs

private const val FORM_COLUMNS = 10
private const val SIDE_COLUMNS = 7

/**
 * Tab of the guest form block; each tab is a page with its own route.
 *
 * @property label the label of the tab.
 * @property view the page of the tab.
 * @since %CURRENT_VERSION%
 */
enum class GuestTab(val label: String, val view: Class<out Component>) {
    /** Signing in by the access code. */
    SignIn("Вход", AuthenticationView::class.java),

    /** Registration of a Student or a Manager. */
    Registration("Регистрация", RegistrationView::class.java),

    /** Restoring access by e-mail. */
    RestoreAccess("Восстановление доступа", RestoreAccessView::class.java),
}

/**
 * Adds the head titled by the [active] tab and a row with the narrow guest form block centered on the page: the tabs of
 * signing in, registration and restoring access with the [active] one selected, and the [content] of the active tab.
 * Choosing another tab opens its page. It must be the first call of the page body, since it adds the head.
 */
internal fun PageScope.guestForm(active: GuestTab, content: BlockScope.() -> Unit) {
    head(active.label)
    guestBlock {
        tabs(initial = active) { GuestTab.entries.forEach { tab -> tab(tab, tab.label) } }
            .onChange { tab -> UI.getCurrent().navigate(tab.view) }
        content()
    }
}

/** Adds a row with the narrow guest block titled [title] centered on the page, which holds the [content]. */
internal fun PageScope.guestBlock(title: String? = null, content: BlockScope.() -> Unit) {
    row {
        space(size = SIDE_COLUMNS)
        block(size = FORM_COLUMNS, title = title, content = content)
        space(size = SIDE_COLUMNS)
    }
}
