package tech.testsys.web.shell

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.server.LoadDependenciesOnStartup
import com.vaadin.flow.theme.lumo.Lumo
import org.springframework.core.env.Environment
import tech.testsys.infra.localization.Localization
import tech.testsys.web.app.config.localization
import tech.testsys.web.app.view.AuthenticationView
import tech.testsys.web.app.view.MainView
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.TestSysTheme
import tech.testsys.web.service.view.ServiceHomeView
import tech.testsys.web.service.view.ServiceLoginView

/**
 * Application shell: loads Lumo and the design system stylesheets on every page, sets the page title to the brand
 * name of the default region, uses the TestSys emblem as favicon and pushes server-side changes to the browser.
 * The start and sign-in pages of both interfaces load their dependencies on bootstrap: the two interfaces share the
 * `""` and `login` paths, so the default choice of eager routes by path would leave one of them without its chunk.
 *
 * @since %CURRENT_VERSION%
 */
@Push
@LoadDependenciesOnStartup(MainView::class, AuthenticationView::class, ServiceHomeView::class, ServiceLoginView::class)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(TestSysTheme.DESIGN_SYSTEM)
@StyleSheet(TestSysTheme.VAADIN_OVERRIDES)
class AppShell(environment: Environment) : AppShellConfigurator {
    private val localization = environment.localization()

    override fun configurePage(settings: AppShellSettings) {
        // The shell page is built before any UI, so it uses the default region rather than the user's one.
        val messages = Localization.forRegion(localization.defaultRegion, localization.defaultTimeZone)
        settings.setPageTitle(messages.uiPage.brandName())
        settings.addBrandFavicon()
    }
}

private fun AppShellSettings.addBrandFavicon() = addFavIcon("icon", TestSysBrand.FAVICON_URL, "any")
