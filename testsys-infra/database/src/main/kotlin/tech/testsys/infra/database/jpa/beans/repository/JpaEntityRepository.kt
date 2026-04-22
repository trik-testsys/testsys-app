package tech.testsys.infra.database.jpa.beans.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import tech.testsys.infra.database.jpa.entity.DescribableJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity

@NoRepositoryBean
interface JpaSequenceEntityRepository<E : SequenceJpaEntity> : JpaRepository<E, Long>

@NoRepositoryBean
interface JpaCompositeEntityRepository<E : CompositeJpaEntity<ID>, ID : CompositeId> : JpaRepository<E, ID>

@NoRepositoryBean
interface JpaDescribableEntityRepository<E : DescribableJpaEntity>