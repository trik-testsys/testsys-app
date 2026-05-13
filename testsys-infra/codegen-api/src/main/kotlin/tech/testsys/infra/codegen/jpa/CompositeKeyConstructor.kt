package tech.testsys.infra.codegen.jpa

/**
 * Marks a `CompositeJpaEntity` subclass to receive a generated top-level "factory constructor"
 * with the same name as the class, taking the flattened fields of the composite id.
 *
 * The annotated class must:
 *  - extend `CompositeJpaEntity<T>` with a single primary constructor parameter `id: T`,
 *  - where `T` is a Kotlin `data class` whose primary constructor parameters are all `val`s.
 *
 * Processed at compile time by `CompositeKeyConstructorProcessor`.
 *
 * @since %CURRENT_VERSION%
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class CompositeKeyConstructor
