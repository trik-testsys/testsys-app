package tech.testsys.infra.database.internal.jpa.repository.task

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskId
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.ExerciseToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.VersionBucketToTaskId
import tech.testsys.infra.database.internal.jpa.entity.task.VersionBucketToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [ExerciseToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ExerciseToTaskContentJpaEntityRepository :
    CompositeJpaEntityRepository<ExerciseToTaskContentJpaEntity, ExerciseToTaskContentId> {

    /**
     * Finds the association rows of the exercise [exerciseId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ExerciseToTaskContentJpaEntity e where e.id.exerciseId = :exerciseId")
    fun findAllByExerciseId(@Param("exerciseId") exerciseId: Long): List<ExerciseToTaskContentJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the exercise [exerciseId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ExerciseToTaskContentJpaEntity e where e.id.exerciseId = :exerciseId")
    fun findAllByExerciseId(@Param("exerciseId") exerciseId: Long, pageable: Pageable): Page<ExerciseToTaskContentJpaEntity>

    /**
     * Finds the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ExerciseToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<ExerciseToTaskContentJpaEntity>

    /**
     * Finds the ids of the task content revisions [taskContentIds] paired with the ids of their exercises in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.taskContentId, e.id.exerciseId) " +
            "from ExerciseToTaskContentJpaEntity e where e.id.taskContentId in :taskContentIds",
    )
    fun findLinkedIdsByTaskContentIdIn(@Param("taskContentIds") taskContentIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from ExerciseToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long, pageable: Pageable): Page<ExerciseToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [TestToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TestToTaskContentJpaEntityRepository :
    CompositeJpaEntityRepository<TestToTaskContentJpaEntity, TestToTaskContentId> {

    /**
     * Finds the association rows of the polygon [testId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TestToTaskContentJpaEntity e where e.id.testId = :testId")
    fun findAllByTestId(@Param("testId") testId: Long): List<TestToTaskContentJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the polygon [testId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TestToTaskContentJpaEntity e where e.id.testId = :testId")
    fun findAllByTestId(@Param("testId") testId: Long, pageable: Pageable): Page<TestToTaskContentJpaEntity>

    /**
     * Finds the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TestToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<TestToTaskContentJpaEntity>

    /**
     * Finds the ids of the task content revisions [taskContentIds] paired with the ids of their polygons in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.taskContentId, e.id.testId) " +
            "from TestToTaskContentJpaEntity e where e.id.taskContentId in :taskContentIds",
    )
    fun findLinkedIdsByTaskContentIdIn(@Param("taskContentIds") taskContentIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TestToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long, pageable: Pageable): Page<TestToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [DeveloperSolutionToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface DeveloperSolutionToTaskContentJpaEntityRepository :
    CompositeJpaEntityRepository<DeveloperSolutionToTaskContentJpaEntity, DeveloperSolutionToTaskContentId> {

    /**
     * Finds the association rows of the developer solution [developerSolutionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.developerSolutionId = :developerSolutionId")
    fun findAllByDeveloperSolutionId(@Param("developerSolutionId") developerSolutionId: Long): List<DeveloperSolutionToTaskContentJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the developer solution [developerSolutionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.developerSolutionId = :developerSolutionId")
    fun findAllByDeveloperSolutionId(
        @Param("developerSolutionId") developerSolutionId: Long,
        pageable: Pageable,
    ): Page<DeveloperSolutionToTaskContentJpaEntity>

    /**
     * Finds the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<DeveloperSolutionToTaskContentJpaEntity>

    /**
     * Finds the ids of the task content revisions [taskContentIds] paired with the ids of their developer solutions
     * in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(" +
            "e.id.taskContentId, e.id.developerSolutionId) " +
            "from DeveloperSolutionToTaskContentJpaEntity e where e.id.taskContentId in :taskContentIds",
    )
    fun findLinkedIdsByTaskContentIdIn(@Param("taskContentIds") taskContentIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(
        @Param("taskContentId") taskContentId: Long,
        pageable: Pageable,
    ): Page<DeveloperSolutionToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [TrikStudioVersionToTaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TrikStudioVersionToTaskContentJpaEntityRepository :
    CompositeJpaEntityRepository<TrikStudioVersionToTaskContentJpaEntity, TrikStudioVersionToTaskContentId> {

    /**
     * Finds the association rows of the TRIK Studio version [trikStudioVersionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.trikStudioVersionId = :trikStudioVersionId")
    fun findAllByTrikStudioVersionId(@Param("trikStudioVersionId") trikStudioVersionId: Long): List<TrikStudioVersionToTaskContentJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the TRIK Studio version [trikStudioVersionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.trikStudioVersionId = :trikStudioVersionId")
    fun findAllByTrikStudioVersionId(
        @Param("trikStudioVersionId") trikStudioVersionId: Long,
        pageable: Pageable,
    ): Page<TrikStudioVersionToTaskContentJpaEntity>

    /**
     * Finds the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<TrikStudioVersionToTaskContentJpaEntity>

    /**
     * Finds the ids of the task content revisions [taskContentIds] paired with the ids of their supported TRIK Studio
     * versions in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(" +
            "e.id.taskContentId, e.id.trikStudioVersionId) " +
            "from TrikStudioVersionToTaskContentJpaEntity e where e.id.taskContentId in :taskContentIds",
    )
    fun findLinkedIdsByTaskContentIdIn(@Param("taskContentIds") taskContentIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the task content revision [taskContentId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(
        @Param("taskContentId") taskContentId: Long,
        pageable: Pageable,
    ): Page<TrikStudioVersionToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [CommunityToTaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CommunityToTaskJpaEntityRepository :
    CompositeJpaEntityRepository<CommunityToTaskJpaEntity, CommunityToTaskId> {

    /**
     * Finds the association rows of any community in the nonempty [communityIds].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdCommunityIdIn(communityIds: Set<Long>): List<CommunityToTaskJpaEntity>

    /**
     * Finds the association rows of the community [communityId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToTaskJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long): List<CommunityToTaskJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the community [communityId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToTaskJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long, pageable: Pageable): Page<CommunityToTaskJpaEntity>

    /**
     * Finds the association rows of the task [taskId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToTaskJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long): List<CommunityToTaskJpaEntity>

    /**
     * Finds the ids of the tasks [taskIds] paired with the ids of the communities they are shared to in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.id.taskId, e.id.communityId) " +
            "from CommunityToTaskJpaEntity e where e.id.taskId in :taskIds",
    )
    fun findLinkedIdsByTaskIdIn(@Param("taskIds") taskIds: Collection<Long>): List<LinkedIdRow>

    /**
     * Finds one [pageable] page of the association rows of the task [taskId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToTaskJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long, pageable: Pageable): Page<CommunityToTaskJpaEntity>
}

/**
 * Spring Data repository for [TaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TaskContentJpaEntityRepository : SnowflakeJpaEntityRepository<TaskContentJpaEntity>

/**
 * Spring Data repository for [TaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TaskJpaEntityRepository : SnowflakeJpaEntityRepository<TaskJpaEntity> {

    /**
     * Locks the task before reading its snapshot, replacing content or removing the task.
     *
     * @since %CURRENT_VERSION%
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from TaskJpaEntity e where e.id = :id")
    fun findLockedById(@Param("id") id: Long): TaskJpaEntity?

    /**
     * Finds the ids of the users [ownerIds] paired with the ids of the tasks they own in one query.
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.ownerId, e.id) " +
            "from TaskJpaEntity e where e.ownerId in :ownerIds",
    )
    fun findLinkedIdsByOwnerIdIn(@Param("ownerIds") ownerIds: Collection<Long>): List<LinkedIdRow>
}

/**
 * Spring Data repository for [VersionBucketToTaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface VersionBucketToTaskJpaEntityRepository :
    CompositeJpaEntityRepository<VersionBucketToTaskJpaEntity, VersionBucketToTaskId> {

    /**
     * Finds the uploaded resource associations of the task [taskId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from VersionBucketToTaskJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long): List<VersionBucketToTaskJpaEntity>

    /**
     * Finds the uploaded resource associations of any of the tasks [taskIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdTaskIdIn(taskIds: Collection<Long>): List<VersionBucketToTaskJpaEntity>
}
