package tech.testsys.infra.diagnostics.internal

/**
 * Opt-in marker for declarations internal to the diagnostics module.
 *
 * @since 1.0.0-beta.1
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
