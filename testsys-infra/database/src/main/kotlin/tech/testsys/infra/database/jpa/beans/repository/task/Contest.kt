package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.CommunityToContestId
import tech.testsys.infra.database.jpa.entity.task.CommunityToContestJpaEntity
import tech.testsys.infra.database.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.jpa.entity.task.TaskToContestId
import tech.testsys.infra.database.jpa.entity.task.TaskToContestJpaEntity

/**
 * Spring Data repository for [TaskToContestJpaEntity] association entities.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
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
interface CommunityToContestJpaEntityRepository : CompositeJpaEntityRepository<CommunityToContestJpaEntity, CommunityToContestId> {

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
interface ContestJpaEntityRepository : SequenceJpaEntityRepository<ContestJpaEntity>
