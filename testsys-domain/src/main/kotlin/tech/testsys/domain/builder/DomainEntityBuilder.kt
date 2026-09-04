package tech.testsys.domain.builder

import tech.testsys.domain.model.EntityVersion
import java.time.Instant

/**
 * DSL marker of the domain entity builder DSL; prevents implicit access to outer receivers in nested blocks.
 *
 * @since %CURRENT_VERSION%
 */
@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class DomainEntityBuilderDsl

/**
 * Builds instances of [T]. Builders check that required fields are set but not the integrity of the data.
 *
 * @param T the type of the built object.
 * @since %CURRENT_VERSION%
 */
@DomainEntityBuilderDsl
interface Builder<out T> {
    /**
     * Builds the instance.
     *
     * @return the built instance.
     * @throws IllegalArgumentException if a required field is not set.
     * @since %CURRENT_VERSION%
     */
    fun build(): T
}

/**
 * Builder of a domain entity.
 *
 * @param Entity the type of the built entity.
 * @property id the raw identifier of the entity, or `null` if not set yet.
 * @property createdAt the creation timestamp of the entity, or `null` if not set yet.
 * @property version the optimistic-lock token of the entity, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
interface DomainEntityBuilder<out Entity> : Builder<Entity> {

    var id: Long?

    var createdAt: Instant?

    var version: EntityVersion?

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
 * Builder that holds a [Data] object configurable through a nested [DataBuilder].
 *
 * @param Data the type of the data object.
 * @param DataBuilder the builder type of [Data].
 * @property data the data object, or `null` if not configured yet.
 * @since %CURRENT_VERSION%
 */
interface DataCapable<Data, DataBuilder : Builder<Data>> {

    var data: Data?

    /**
     * Creates a new [DataBuilder].
     *
     * @return a fresh, unconfigured data builder.
     * @since %CURRENT_VERSION%
     */
    fun dataBuilder(): DataBuilder
}

/**
 * Configures [DataCapable.data] with a nested builder block.
 *
 * @param Data the type of the data object.
 * @param DataBuilder the builder type of [Data].
 * @param builder the configuration block applied to a fresh [DataBuilder].
 * @since %CURRENT_VERSION%
 */
inline fun <Data, DataBuilder : Builder<Data>> DataCapable<Data, DataBuilder>.data(builder: DataBuilder.() -> Unit) {
    data = dataBuilder().apply(builder).build()
}

/**
 * Base class of domain entity builders with a data object; [id], [createdAt], [version] and [data] start as `null`.
 *
 * @param Entity the type of the built entity.
 * @param Data the type of the data object.
 * @param DataBuilder the builder type of [Data].
 * @since %CURRENT_VERSION%
 */
abstract class DomainEntityWithDataBuilder<Entity, Data, DataBuilder : Builder<Data>> :
    DomainEntityBuilder<Entity>, DataCapable<Data, DataBuilder> {

    override var id: Long? = null
    override var createdAt: Instant? = null
    override var version: EntityVersion? = null
    override var data: Data? = null
}
