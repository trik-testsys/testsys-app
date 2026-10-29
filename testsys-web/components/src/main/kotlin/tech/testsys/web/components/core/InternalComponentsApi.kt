package tech.testsys.web.components.core

/** Marks implementation details of the components module. */
@RequiresOptIn
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class InternalComponentsApi
