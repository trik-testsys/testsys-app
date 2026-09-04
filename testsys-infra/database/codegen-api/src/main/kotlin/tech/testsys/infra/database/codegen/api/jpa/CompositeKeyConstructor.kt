package tech.testsys.infra.database.codegen.api.jpa

/**
 * Marks a `CompositeJpaEntity<T>` subclass to receive a generated top-level factory function named after the class
 * and taking the fields of its composite id. The class must have a single primary constructor parameter `id: T`, where `T`
 * is a `data class` with `val`-only primary constructor parameters.
 *
 * @since %CURRENT_VERSION%
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CompositeKeyConstructor
