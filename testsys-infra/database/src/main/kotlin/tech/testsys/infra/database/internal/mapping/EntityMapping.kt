package tech.testsys.infra.database.internal.mapping

import tech.testsys.domain.model.DomainEntity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.JpaEntity

/**
 * Marker for the mapping between a domain entity type and the JPA entity type persisting it.
 *
 * @param Domain the domain entity type.
 * @param Jpa the JPA entity type.
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
interface EntityMapping<Domain : DomainEntity<*>, Jpa : JpaEntity>
