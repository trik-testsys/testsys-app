package tech.testsys.web

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.lumo.Lumo
import tech.testsys.web.ui.TestSysTheme
import tech.testsys.web.ui.UiTexts

/**
 * Application shell: loads Lumo and the design system stylesheets on every page and sets the page title
 * to [UiTexts.brand].
 *
 * @since %CURRENT_VERSION%
 */
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(TestSysTheme.DESIGN_SYSTEM)
@StyleSheet(TestSysTheme.VAADIN_OVERRIDES)
class AppShell(private val texts: UiTexts) : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings) {
        settings.setPageTitle(texts.brand)
    }
}
