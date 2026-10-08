package tech.testsys.domain.model.group

/**
 * Marks behavior that requires the stored invite code to equal the original issued code, as Identity does today.
 * Every marked declaration must be checked and corrected before enabling another hash algorithm.
 *
 * @property reason why the declaration requires the original invite code.
 * @since %CURRENT_VERSION%
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class RawInviteCodeDependency(val reason: String)
