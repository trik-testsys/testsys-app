package tech.testsys.infra.localization

/**
 * Opt-in marker for declarations internal to the localization module; callers outside it must opt in explicitly.
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
annotation class InternalLocalizationApi
