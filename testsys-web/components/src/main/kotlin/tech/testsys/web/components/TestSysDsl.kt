package tech.testsys.web.components

/**
 * DSL marker of the design system builders: an inner scope cannot implicitly call the builders of an outer one.
 *
 * @since 1.0.0-beta.1
 */
@DslMarker
annotation class TestSysDsl
