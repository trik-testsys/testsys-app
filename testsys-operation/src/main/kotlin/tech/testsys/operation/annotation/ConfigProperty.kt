package tech.testsys.operation.annotation

/**
 * Marks a property of an [OperationConfig] interface as a configuration property; a property without it is static
 * with the derived name.
 *
 * @property name the key segment of the property; empty means the kebab-case of the property name.
 * @property isDynamic `true` if the value may change between calls, `false` if it is fixed for the application
 * lifetime.
 * @since %CURRENT_VERSION%
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ConfigProperty(
    val name: String = "",
    val isDynamic: Boolean = false,
)
