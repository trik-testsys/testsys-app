package tech.testsys.operation.annotation

/**
 * Marks an interface as a configuration of operations.
 *
 * @property name the name of the configuration, the key segment in `testsys.operation.<name>.<property>`.
 * @since %CURRENT_VERSION%
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class OperationConfig(val name: String)
