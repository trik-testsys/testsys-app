package tech.testsys.web.shell

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.lumo.Lumo
import org.springframework.core.env.Environment
import tech.testsys.infra.localization.Localization
import tech.testsys.web.app.config.defaultRegion
import tech.testsys.web.app.config.defaultTimeZone
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.TestSysTheme

/**
 * Application shell: loads Lumo and the design system stylesheets on every page, sets the page title to the brand
 * name of the default region, uses the TestSys emblem as favicon and pushes server-side changes to the browser.
 *
 * @since %CURRENT_VERSION%
 */
@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(TestSysTheme.DESIGN_SYSTEM)
@StyleSheet(TestSysTheme.VAADIN_OVERRIDES)
class AppShell(private val environment: Environment) : AppShellConfigurator {
    override fun configurePage(settings: AppShellSettings) {
        // The shell page is built before any UI, so it uses the default region rather than the user's one.
        val messages = Localization.forRegion(environment.defaultRegion(), environment.defaultTimeZone())
        settings.setPageTitle(messages.uiPage.brandName())
        settings.addBrandFavicon()
    }
}

private fun AppShellSettings.addBrandFavicon() = addFavIcon("icon", TestSysBrand.FAVICON_URL, "any")
