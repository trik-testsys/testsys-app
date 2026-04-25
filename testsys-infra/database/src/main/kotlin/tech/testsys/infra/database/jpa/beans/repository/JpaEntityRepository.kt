package tech.testsys.infra.database.jpa.beans.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.entity.CompositeJpaEntity
import tech.testsys.infra.database.jpa.entity.CompositeId
import tech.testsys.infra.database.jpa.entity.SequenceJpaEntity
import tech.testsys.infra.database.jpa.entity.group.ClassJpaEntity

@NoRepositoryBean
interface SequenceJpaEntityRepository<Entity : SequenceJpaEntity> : JpaRepository<Entity, Long>

@NoRepositoryBean
interface CompositeJpaEntityRepository<Entity : CompositeJpaEntity<ID>, ID : CompositeId> : JpaRepository<Entity, ID>

@Repository
interface ClassRepository : SequenceJpaEntityRepository<ClassJpaEntity> {

}