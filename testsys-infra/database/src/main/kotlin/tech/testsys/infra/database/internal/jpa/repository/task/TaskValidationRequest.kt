package tech.testsys.infra.database.internal.jpa.repository.task

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.*
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository of [TaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TaskValidationRequestJpaEntityRepository : SnowflakeJpaEntityRepository<TaskValidationRequestJpaEntity> {
    /**
     * Finds a request for a short atomic write.
     *
     * @since %CURRENT_VERSION%
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from TaskValidationRequestJpaEntity e where e.id = :id")
    fun findLockedById(@Param("id") id: Long): TaskValidationRequestJpaEntity?

    /**
     * Finds the saved history of a task.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByTaskIdOrderByCreatedAtAscIdAsc(taskId: Long): List<TaskValidationRequestJpaEntity>

    /**
     * Finds the requests of the task [taskId] in any of the [executions] stages, in the order of the history.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByTaskIdAndExecutionInOrderByCreatedAtAscIdAsc(
        taskId: Long,
        executions: Collection<TaskValidationExecutionJpaEnum>,
    ): List<TaskValidationRequestJpaEntity>

    /**
     * Finds requests in any of the given execution stages.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByExecutionInOrderByIdAsc(executions: Collection<TaskValidationExecutionJpaEnum>): List<TaskValidationRequestJpaEntity>
}

/**
 * Spring Data repository of [TestToTaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TestToTaskValidationRequestJpaEntityRepository :
    CompositeJpaEntityRepository<TestToTaskValidationRequestJpaEntity, TestToTaskValidationRequestId> {
    /**
     * Finds the pinned inputs or references of a request.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestId(requestId: Long): List<TestToTaskValidationRequestJpaEntity>

    /**
     * Finds the pinned polygons of any of the requests [requestIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestIdIn(requestIds: Collection<Long>): List<TestToTaskValidationRequestJpaEntity>

    /**
     * Deletes the pinned polygons of the request [requestId] in one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from TestToTaskValidationRequestJpaEntity e where e.id.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}

/**
 * Spring Data repository of [DeveloperSolutionToTaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface DeveloperSolutionToTaskValidationRequestJpaEntityRepository :
    CompositeJpaEntityRepository<DeveloperSolutionToTaskValidationRequestJpaEntity, DeveloperSolutionToTaskValidationRequestId> {
    /**
     * Finds the pinned developer solutions of any of the requests [requestIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestIdIn(requestIds: Collection<Long>): List<DeveloperSolutionToTaskValidationRequestJpaEntity>

    /**
     * Deletes the pinned developer solutions of the request [requestId] in one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from DeveloperSolutionToTaskValidationRequestJpaEntity e where e.id.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}

/**
 * Spring Data repository of [TrikStudioVersionToTaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TrikStudioVersionToTaskValidationRequestJpaEntityRepository :
    CompositeJpaEntityRepository<TrikStudioVersionToTaskValidationRequestJpaEntity, TrikStudioVersionToTaskValidationRequestId> {
    /**
     * Finds the pinned TRIK Studio versions of any of the requests [requestIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestIdIn(requestIds: Collection<Long>): List<TrikStudioVersionToTaskValidationRequestJpaEntity>

    /**
     * Deletes the pinned TRIK Studio versions of the request [requestId] in one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from TrikStudioVersionToTaskValidationRequestJpaEntity e where e.id.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}

/**
 * Spring Data repository of [SubmissionToTaskValidationRequestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface SubmissionToTaskValidationRequestJpaEntityRepository :
    CompositeJpaEntityRepository<SubmissionToTaskValidationRequestJpaEntity, SubmissionToTaskValidationRequestId> {
    /**
     * Finds links in their stored author solution and version order.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestIdOrderByPositionAsc(requestId: Long): List<SubmissionToTaskValidationRequestJpaEntity>

    /**
     * Finds the links of any of the requests [requestIds] in one query, in their stored order within each request.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestIdInOrderByPositionAsc(requestIds: Collection<Long>): List<SubmissionToTaskValidationRequestJpaEntity>

    /**
     * Finds validation requests referencing a submission.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdSubmissionId(submissionId: Long): List<SubmissionToTaskValidationRequestJpaEntity>

    /**
     * Deletes the submission links of the request [requestId] in one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from SubmissionToTaskValidationRequestJpaEntity e where e.id.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}
