package tech.testsys.infra.database.internal.mapping

import tech.testsys.domain.model.DomainEntity
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.JpaEntity

@InternalDatabaseApi
interface EntityMapping<Domain : DomainEntity<*>, Jpa : JpaEntity>
