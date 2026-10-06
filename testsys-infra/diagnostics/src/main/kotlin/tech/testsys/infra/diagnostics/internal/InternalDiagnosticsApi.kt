package tech.testsys.infra.diagnostics.internal

/**
 * Opt-in marker for declarations internal to the diagnostics module.
 *
 * @since %CURRENT_VERSION%
 */
@RequiresOptIn
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.BINARY)
annotation class InternalDiagnosticsApi
