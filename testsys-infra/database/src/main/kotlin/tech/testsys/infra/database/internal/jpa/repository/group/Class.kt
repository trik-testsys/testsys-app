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
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [StudentToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface StudentToClassJpaEntityRepository : CompositeJpaEntityRepository<StudentToClassJpaEntity, StudentToClassId> {

    /**
     * Finds the association rows of the student [studentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from StudentToClassJpaEntity e where e.id.studentId = :studentId")
    fun findAllByStudentId(@Param("studentId") studentId: Long): List<StudentToClassJpaEntity>

    /**
     * Finds the ids of the students [studentIds] paired with the ids of the classes they are enrolled in in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.studentId, e.id.classId) " +
            "from StudentToClassJpaEntity e where e.id.studentId in :studentIds",
    )
    fun findLinkedIdsByStudentIdIn(@Param("studentIds") studentIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the student [studentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from StudentToClassJpaEntity e where e.id.studentId = :studentId")
    fun findAllByStudentId(@Param("studentId") studentId: Long, pageable: Pageable): Page<StudentToClassJpaEntity>

    /**
     * Finds the association rows of the class [classId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from StudentToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long): List<StudentToClassJpaEntity>

    /**
     * Finds the ids of the classes [classIds] paired with the ids of their students in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.classId, e.id.studentId) " +
            "from StudentToClassJpaEntity e where e.id.classId in :classIds",
    )
    fun findLinkedIdsByClassIdIn(@Param("classIds") classIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the class [classId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from StudentToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long, pageable: Pageable): Page<StudentToClassJpaEntity>
}

/**
 * Spring Data repository for [ContestToClassJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ContestToClassJpaEntityRepository : CompositeJpaEntityRepository<ContestToClassJpaEntity, ContestToClassId> {

    /**
     * Finds the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToClassJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<ContestToClassJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToClassJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<ContestToClassJpaEntity>

    /**
     * Finds the association rows of the class [classId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ContestToClassJpaEntity e where e.id.classId = :classId")
    fun findAllByClassId(@Param("classId") classId: Long): List<ContestToClassJpaEntity>

    /**
     * Finds the ids of the classes [classIds] paired with the ids of their contests in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.classId, e.id.contestId) " +
            "from ContestToClassJpaEntity e where e.id.classId in :classIds",
    )
    fun findLinkedIdsByClassIdIn(@Param("classIds") classIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the class [classId].
     *
     * @since %CURRENT_VERSION%
     */
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
interface ClassJpaEntityRepository : SnowflakeJpaEntityRepository<ClassJpaEntity> {

    /**
     * Finds the ids of the users [ownerIds] paired with the ids of the classes they own in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.ownerId, e.id) " +
            "from ClassJpaEntity e where e.ownerId in :ownerIds",
    )
    fun findLinkedIdsByOwnerIdIn(@Param("ownerIds") ownerIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds the class referencing the invite [inviteId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findByInviteId(inviteId: Long): ClassJpaEntity?
}
