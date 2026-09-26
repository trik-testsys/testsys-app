package tech.testsys.web

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.lumo.Lumo
import tech.testsys.web.ui.TestSysTheme

/**
 * Application shell: loads Lumo and the design system stylesheets on every page.
 *
 * @since %CURRENT_VERSION%
 */
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(TestSysTheme.DESIGN_SYSTEM)
@StyleSheet(TestSysTheme.VAADIN_OVERRIDES)
class AppShell : AppShellConfigurator
