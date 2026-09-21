package tech.testsys.operation.annotation

/**
 * Marks an operation as the implementation of a user feature.
 *
 * @property identifier the codifier of the feature, e.g. `testsys.user.multi.developer.task.createTask`.
 * @since %CURRENT_VERSION%
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Feature(val identifier: String)
