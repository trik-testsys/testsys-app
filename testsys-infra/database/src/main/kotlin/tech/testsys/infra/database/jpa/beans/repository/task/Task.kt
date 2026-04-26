package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.CommunityToTaskId
import tech.testsys.infra.database.jpa.entity.task.CommunityToTaskJpaEntity
import tech.testsys.infra.database.jpa.entity.task.DeveloperSolutionToTaskContentId
import tech.testsys.infra.database.jpa.entity.task.DeveloperSolutionToTaskContentJpaEntity
import tech.testsys.infra.database.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.jpa.entity.task.TaskJpaEntity
import tech.testsys.infra.database.jpa.entity.task.TestToTaskContentId
import tech.testsys.infra.database.jpa.entity.task.TestToTaskContentJpaEntity
import tech.testsys.infra.database.jpa.entity.task.TrikStudioVersionToTaskContentId
import tech.testsys.infra.database.jpa.entity.task.TrikStudioVersionToTaskContentJpaEntity

/**
 * Spring Data repository for [TestToTaskContentJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface TestToTaskContentJpaEntityRepository : CompositeJpaEntityRepository<TestToTaskContentJpaEntity, TestToTaskContentId> {

    @Query("select e from TestToTaskContentJpaEntity e where e.id.testId = :testId")
    fun findAllByTestId(@Param("testId") testId: Long): List<TestToTaskContentJpaEntity>

    @Query("select e from TestToTaskContentJpaEntity e where e.id.testId = :testId")
    fun findAllByTestId(@Param("testId") testId: Long, pageable: Pageable): Page<TestToTaskContentJpaEntity>

    @Query("select e from TestToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<TestToTaskContentJpaEntity>

    @Query("select e from TestToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long, pageable: Pageable): Page<TestToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [DeveloperSolutionToTaskContentJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface DeveloperSolutionToTaskContentJpaEntityRepository :
    CompositeJpaEntityRepository<DeveloperSolutionToTaskContentJpaEntity, DeveloperSolutionToTaskContentId> {

    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.developerSolutionId = :developerSolutionId")
    fun findAllByDeveloperSolutionId(@Param("developerSolutionId") developerSolutionId: Long): List<DeveloperSolutionToTaskContentJpaEntity>

    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.developerSolutionId = :developerSolutionId")
    fun findAllByDeveloperSolutionId(
        @Param("developerSolutionId") developerSolutionId: Long,
        pageable: Pageable,
    ): Page<DeveloperSolutionToTaskContentJpaEntity>

    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<DeveloperSolutionToTaskContentJpaEntity>

    @Query("select e from DeveloperSolutionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(
        @Param("taskContentId") taskContentId: Long,
        pageable: Pageable,
    ): Page<DeveloperSolutionToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [TrikStudioVersionToTaskContentJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface TrikStudioVersionToTaskContentJpaEntityRepository :
    CompositeJpaEntityRepository<TrikStudioVersionToTaskContentJpaEntity, TrikStudioVersionToTaskContentId> {

    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.trikStudioVersionId = :trikStudioVersionId")
    fun findAllByTrikStudioVersionId(@Param("trikStudioVersionId") trikStudioVersionId: Long): List<TrikStudioVersionToTaskContentJpaEntity>

    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.trikStudioVersionId = :trikStudioVersionId")
    fun findAllByTrikStudioVersionId(
        @Param("trikStudioVersionId") trikStudioVersionId: Long,
        pageable: Pageable,
    ): Page<TrikStudioVersionToTaskContentJpaEntity>

    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(@Param("taskContentId") taskContentId: Long): List<TrikStudioVersionToTaskContentJpaEntity>

    @Query("select e from TrikStudioVersionToTaskContentJpaEntity e where e.id.taskContentId = :taskContentId")
    fun findAllByTaskContentId(
        @Param("taskContentId") taskContentId: Long,
        pageable: Pageable,
    ): Page<TrikStudioVersionToTaskContentJpaEntity>
}

/**
 * Spring Data repository for [CommunityToTaskJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface CommunityToTaskJpaEntityRepository : CompositeJpaEntityRepository<CommunityToTaskJpaEntity, CommunityToTaskId> {

    @Query("select e from CommunityToTaskJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long): List<CommunityToTaskJpaEntity>

    @Query("select e from CommunityToTaskJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long, pageable: Pageable): Page<CommunityToTaskJpaEntity>

    @Query("select e from CommunityToTaskJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long): List<CommunityToTaskJpaEntity>

    @Query("select e from CommunityToTaskJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long, pageable: Pageable): Page<CommunityToTaskJpaEntity>
}

/**
 * Spring Data repository for [TaskContentJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface TaskContentJpaEntityRepository : SequenceJpaEntityRepository<TaskContentJpaEntity>

/**
 * Spring Data repository for [TaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface TaskJpaEntityRepository : SequenceJpaEntityRepository<TaskJpaEntity>
