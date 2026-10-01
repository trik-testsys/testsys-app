package tech.testsys.web.devapp

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.lumo.Lumo
import tech.testsys.web.components.TestSysTheme
import tech.testsys.web.components.UiTexts

/**
 * Application shell: loads Lumo and the design system stylesheets on every page, sets the page title
 * to [UiTexts.brand] and pushes server-side changes to the browser.
 *
 * @since %CURRENT_VERSION%
 */
@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(TestSysTheme.DESIGN_SYSTEM)
@StyleSheet(TestSysTheme.VAADIN_OVERRIDES)
class AppShell(private val texts: UiTexts) : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings) {
        settings.setPageTitle(texts.brand)
    }
}
