package tech.testsys.domain.builder

import java.time.Instant

/**
 * DSL marker annotation for domain entity builder DSLs.
 * Prevents implicit access to outer builder receivers in nested builder blocks.
 *
 * @since %CURRENT_VERSION%
 */
@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class DomainEntityBuilderDsl

/**
 * Base interface for all builders that construct instances of [T].
 *
 * NOTE: Builder does not check any data integrity when constructing the [T]
 *
 * @param T the type of object this builder produces.
 * @since %CURRENT_VERSION%
 */
@DomainEntityBuilderDsl
interface Builder<out T> {
    /**
     * Constructs and returns the resulting instance of [T].
     *
     * @return the built instance.
     * @throws IllegalArgumentException if any required fields are not set.
     * @since %CURRENT_VERSION%
     */
    fun build(): T
}

/**
 * Builder interface for domain entities that have an [id] and [createdAt] timestamp.
 *
 * @param Entity the type of domain entity this builder produces.
 * @since %CURRENT_VERSION%
 */
interface DomainEntityBuilder<out Entity> : Builder<Entity> {

    /**
     * The unique identifier for the domain entity, or `null` if not yet assigned.
     *
     * @since %CURRENT_VERSION%
     */
    var id: Long?

    /**
     * The creation timestamp for the domain entity, or `null` if not yet assigned.
     *
     * @since %CURRENT_VERSION%
     */
    var createdAt: Instant?

    /**
     * Sets [createdAt] to the current instant.
     *
     * @since %CURRENT_VERSION%
     */
    fun createdNow() {
        createdAt = Instant.now()
    }

}

/**
 * Interface for builders that support configuring an associated [Data] object
 * via a nested [DataBuilder].
 *
 * @param Data the type of data object.
 * @param DataBuilder the builder type used to construct [Data].
 * @since %CURRENT_VERSION%
 */
interface DataCapable<Data, DataBuilder : Builder<Data>> {

    /**
     * The data object, or `null` if not yet configured.
     *
     * @since %CURRENT_VERSION%
     */
    var data: Data?

    /**
     * Creates a new instance of [DataBuilder] for configuring the data object.
     *
     * @return a new data builder instance.
     * @since %CURRENT_VERSION%
     */
    fun dataBuilder(): DataBuilder

}

/**
 * DSL function for configuring the [data][DataCapable.data] of a [DataCapable] builder
 * using a lambda applied to the [DataBuilder].
 *
 * @param builder the configuration block applied to the data builder.
 * @since %CURRENT_VERSION%
 */
inline fun <Data, DataBuilder : Builder<Data>> DataCapable<Data, DataBuilder>.data(builder: DataBuilder.() -> Unit) {
    data = dataBuilder().apply(builder).build()
}

/**
 * Abstract base class for domain entity builders that include an associated data object.
 * Provides default `null` initial values for [id], [createdAt], and [data].
 *
 * @param Entity the type of domain entity this builder produces.
 * @param Data the type of associated data object.
 * @param DataBuilder the builder type used to construct [Data].
 * @since %CURRENT_VERSION%
 */
abstract class DomainEntityWithDataBuilder<Entity, Data, DataBuilder : Builder<Data>> :
    DomainEntityBuilder<Entity>, DataCapable<Data, DataBuilder>
{

    override var id: Long? = null
    override var createdAt: Instant? = null
    override var data: Data? = null

}
