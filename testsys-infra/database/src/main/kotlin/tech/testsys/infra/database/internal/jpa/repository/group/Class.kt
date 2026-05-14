package tech.testsys.infra.database.internal.jpa.repository.group

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToClassId
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.StudentToClassId
import tech.testsys.infra.database.internal.jpa.entity.group.StudentToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [StudentToClassJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface StudentToClassJpaEntityRepository : CompositeJpaEntityRepository<StudentToClassJpaEntity, StudentToClassId> {

    @Query("select e from StudentToClassJpaEntity e where e.id.studentId = :studentId")
    fun findAllByStudentId(@Param("studentId") studentId: Long): List<StudentToClassJpaEntity>

    @Query("select e from StudentToClassJpaEntity e where e.id.studentId = :studentId")
    fun findAllByStudentId(@Param("studentId") studentId: Long, pageable: Pageable): Page<StudentToClassJpaEntity>

    @Query("select e from StudentToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long): List<StudentToClassJpaEntity>

    @Query("select e from StudentToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long, pageable: Pageable): Page<StudentToClassJpaEntity>
}

/**
 * Spring Data repository for [ContestToClassJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ContestToClassJpaEntityRepository : CompositeJpaEntityRepository<ContestToClassJpaEntity, ContestToClassId> {

    @Query("select e from ContestToClassJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<ContestToClassJpaEntity>

    @Query("select e from ContestToClassJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<ContestToClassJpaEntity>

    @Query("select e from ContestToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long): List<ContestToClassJpaEntity>

    @Query("select e from ContestToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long, pageable: Pageable): Page<ContestToClassJpaEntity>
}

/**
 * Spring Data repository for [ClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ClassJpaEntityRepository : SequenceJpaEntityRepository<ClassJpaEntity> {

    fun findAllByOwnerId(ownerId: Long): List<ClassJpaEntity>
}
