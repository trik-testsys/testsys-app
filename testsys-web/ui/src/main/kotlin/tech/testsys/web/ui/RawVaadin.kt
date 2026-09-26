package tech.testsys.web.ui

/**
 * Marks the escape hatch that puts a raw Vaadin component into a page past the design system rules.
 *
 * @since %CURRENT_VERSION%
 */
@RequiresOptIn(message = "Raw Vaadin components bypass the design system rules", level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class RawVaadin
