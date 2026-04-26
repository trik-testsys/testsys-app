package tech.testsys.infra.database.jpa.beans.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.NoRepositoryBean
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

/**
 * Base Spring Data repository for entities with a simple [Long] primary key
 * (subclasses of [SequenceJpaEntity]).
 *
 * Marked [NoRepositoryBean] so Spring does not try to instantiate it directly;
 * concrete sub-interfaces become repository beans.
 *
 * Mixes in [JpaSpecificationExecutor] so all concrete sub-interfaces support
 * dynamic Criteria-API queries via `Specification<Entity>`.
 *
 * @param Entity the concrete entity type stored by this repository.
 * @since %CURRENT_VERSION%
 */
@NoRepositoryBean
interface SequenceJpaEntityRepository<Entity : SequenceJpaEntity> :
    JpaRepository<Entity, Long>,
    JpaSpecificationExecutor<Entity>

/**
 * Base Spring Data repository for entities with a composite primary key
 * (subclasses of [CompositeJpaEntity]).
 *
 * Marked [NoRepositoryBean] so Spring does not try to instantiate it directly;
 * concrete sub-interfaces become repository beans.
 *
 * Mixes in [JpaSpecificationExecutor] so all concrete sub-interfaces support
 * dynamic Criteria-API queries via `Specification<Entity>`.
 *
 * @param Entity the concrete entity type stored by this repository.
 * @param ID the composite key type, must extend [CompositeId].
 * @since %CURRENT_VERSION%
 */
@NoRepositoryBean
interface CompositeJpaEntityRepository<Entity : CompositeJpaEntity<ID>, ID : CompositeId> :
    JpaRepository<Entity, ID>,
    JpaSpecificationExecutor<Entity>
