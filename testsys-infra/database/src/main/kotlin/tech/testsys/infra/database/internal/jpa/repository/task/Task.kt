package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskId
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentId
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

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
interface TaskContentJpaEntityRepository : SequenceJpaEntityRepository<TaskContentJpaEntity>

/**
 * Spring Data repository for [TaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TaskJpaEntityRepository : SequenceJpaEntityRepository<TaskJpaEntity> {

    /**
     * Finds the tasks owned by the user [ownerId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByOwnerId(ownerId: Long): List<TaskJpaEntity>
}
