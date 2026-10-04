package tech.testsys.web.components

/**
 * Stylesheets of the design system to load after the Lumo theme, in the order of the properties.
 *
 * @property DESIGN_SYSTEM tokens and `.ts-*` classes packaged as public UI resources.
 * @property VAADIN_OVERRIDES the mapping of Lumo and Vaadin components onto the design system tokens.
 * @since %CURRENT_VERSION%
 */
object TestSysTheme {
    const val DESIGN_SYSTEM: String = "testsys-ui/styles/styles.css"
    const val VAADIN_OVERRIDES: String = "testsys-ui/styles/vaadin.css"
}
