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
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [TaskToContestJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TaskToContestJpaEntityRepository : CompositeJpaEntityRepository<TaskToContestJpaEntity, TaskToContestId> {

    @Query("select e from TaskToContestJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long): List<TaskToContestJpaEntity>

    @Query("select e from TaskToContestJpaEntity e where e.id.taskId = :taskId")
    fun findAllByTaskId(@Param("taskId") taskId: Long, pageable: Pageable): Page<TaskToContestJpaEntity>

    @Query("select e from TaskToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<TaskToContestJpaEntity>

    @Query("select e from TaskToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long, pageable: Pageable): Page<TaskToContestJpaEntity>
}

/**
 * Spring Data repository for [CommunityToContestJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface CommunityToContestJpaEntityRepository :
    CompositeJpaEntityRepository<CommunityToContestJpaEntity, CommunityToContestId> {

    @Query("select e from CommunityToContestJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long): List<CommunityToContestJpaEntity>

    @Query("select e from CommunityToContestJpaEntity e where e.id.communityId = :communityId")
    fun findAllByCommunityId(@Param("communityId") communityId: Long, pageable: Pageable): Page<CommunityToContestJpaEntity>

    @Query("select e from CommunityToContestJpaEntity e where e.id.contestId = :contestId")
    fun findAllByContestId(@Param("contestId") contestId: Long): List<CommunityToContestJpaEntity>

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
interface ContestJpaEntityRepository : SequenceJpaEntityRepository<ContestJpaEntity> {

    fun findAllByOwnerId(ownerId: Long): List<ContestJpaEntity>
}
