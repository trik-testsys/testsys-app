package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestId
import tech.testsys.infra.database.internal.jpa.entity.task.CommunityToContestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskToContestId
import tech.testsys.infra.database.internal.jpa.entity.task.TaskToContestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [TaskToContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TaskToContestJpaEntityRepository : CompositeJpaEntityRepository<TaskToContestJpaEntity, TaskToContestId> {

    /**
     * Finds the association rows of the task [taskId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TaskToContestJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long): List<TaskToContestJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the task [taskId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TaskToContestJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long, pageable: Pageable): Page<TaskToContestJpaEntity>

    /**
     * Finds the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TaskToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<TaskToContestJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from TaskToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<TaskToContestJpaEntity>
}

/**
 * Spring Data repository for [CommunityToContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CommunityToContestJpaEntityRepository :
    CompositeJpaEntityRepository<CommunityToContestJpaEntity, CommunityToContestId> {

    /**
     * Finds the association rows of the community [communityId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToContestJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long): List<CommunityToContestJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the community [communityId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToContestJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long, pageable: Pageable): Page<CommunityToContestJpaEntity>

    /**
     * Finds the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<CommunityToContestJpaEntity>

    /**
     * Finds one [pageable] page of the association rows of the contest [contestId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query("select e from CommunityToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<CommunityToContestJpaEntity>
}

/**
 * Spring Data repository for [ContestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface ContestJpaEntityRepository : SnowflakeJpaEntityRepository<ContestJpaEntity> {

    /**
     * Finds the contests owned by the user [ownerId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByOwnerId(ownerId: Long): List<ContestJpaEntity>
}
