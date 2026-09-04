package tech.testsys.infra.database.internal.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.CompositeId
import tech.testsys.infra.database.internal.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * Base Spring Data repository of [SequenceJpaEntity] subtypes with [JpaSpecificationExecutor] support;
 * [NoRepositoryBean], so only the concrete sub-interfaces become beans.
 *
 * @param Entity the stored entity type.
 * @since %CURRENT_VERSION%
 */
@NoRepositoryBean
@InternalDatabaseApi
interface SequenceJpaEntityRepository<Entity : SequenceJpaEntity> :
    JpaRepository<Entity, Long>,
    JpaSpecificationExecutor<Entity>

/**
 * Base Spring Data repository of [CompositeJpaEntity] subtypes with [JpaSpecificationExecutor] support;
 * [NoRepositoryBean], so only the concrete sub-interfaces become beans.
 *
 * @param Entity the stored entity type.
 * @param ID the composite key type.
 * @since %CURRENT_VERSION%
 */
@NoRepositoryBean
@InternalDatabaseApi
interface CompositeJpaEntityRepository<Entity : CompositeJpaEntity<ID>, ID : CompositeId> :
    JpaRepository<Entity, ID>,
    JpaSpecificationExecutor<Entity>
