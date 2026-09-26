package tech.testsys.web.ui

/**
 * DSL marker of the design system builders: an inner scope cannot implicitly call the builders of an outer one.
 *
 * @since %CURRENT_VERSION%
 */
@DslMarker
annotation class TestSysDsl
