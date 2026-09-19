package tech.testsys.operation.annotation

/**
 * Opt-in marker for declarations internal to the operation module; callers outside it must opt in explicitly.
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
annotation class InternalOperationsApi
