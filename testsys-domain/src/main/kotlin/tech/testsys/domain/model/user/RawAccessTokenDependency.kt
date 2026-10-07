package tech.testsys.domain.model.user

/**
 * Marks behavior that requires the stored access code to equal the original user-provided code, as Identity does today.
 * Every marked declaration must be checked and corrected before enabling another hash algorithm.
 *
 * @property reason why the declaration requires the original access code.
 * @since %CURRENT_VERSION%
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class RawAccessTokenDependency(val reason: String)
