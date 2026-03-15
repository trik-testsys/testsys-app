package tech.testsys.domain.builder.util

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.TrikSupportedLanguage

/**
 * Wraps a list of domain IDs into a [LazyEntityList] for deferred entity resolution.
 *
 * @return a [LazyEntityList] backed by this list of IDs.
 * @since %CURRENT_VERSION%
 */
fun <Id : DomainId, Entity : DomainEntity<Id>> List<Id>.lazify() =
    LazyEntityList<Id, Entity>(this)

/**
 * Wraps a single domain ID into a [LazyEntity] for deferred entity resolution.
 *
 * @return a [LazyEntity] backed by this ID.
 * @since %CURRENT_VERSION%
 */
fun <Id : DomainId, Entity : DomainEntity<Id>> Id.lazify() =
    LazyEntity<Id, Entity>(this)

internal fun <T> Builder<*>.requireField(value: T?, fieldName: String): T = requireNotNull(value) {
    "${this::class.simpleName}: required field '$fieldName' was not set"
}
