package tech.testsys.domain.builder.util

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import kotlin.reflect.KProperty0

/**
 * Wraps this list of ids into a [LazyEntityList].
 *
 * @param Id the identifier type of the referenced entities.
 * @param Entity the type of the referenced entities.
 * @since %CURRENT_VERSION%
 */
fun <Id : DomainId, Entity : DomainEntity<Id>> List<Id>.lazify() = LazyEntityList<Id, Entity>(this)

/**
 * Wraps this id into a [LazyEntity].
 *
 * @param Id the identifier type of the referenced entity.
 * @param Entity the type of the referenced entity.
 * @since %CURRENT_VERSION%
 */
fun <Id : DomainId, Entity : DomainEntity<Id>> Id.lazify() = LazyEntity<Id, Entity>(this)

internal fun <T> Builder<*>.requireField(value: T?, lazyField: () -> KProperty0<T?>): T = requireNotNull(value) {
    "${this::class.simpleName}: required field '${lazyField.invoke().name}' was not set"
}
