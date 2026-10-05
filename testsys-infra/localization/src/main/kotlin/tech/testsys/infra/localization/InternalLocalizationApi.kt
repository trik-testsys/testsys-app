package tech.testsys.infra.localization

/**
 * Opt-in marker for declarations internal to the localization module; callers outside it must opt in explicitly.
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
annotation class InternalLocalizationApi
