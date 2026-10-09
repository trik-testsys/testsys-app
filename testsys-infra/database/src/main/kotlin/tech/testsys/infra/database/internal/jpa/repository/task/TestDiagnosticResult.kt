package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DiagnosticReportJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestDiagnosticResultId
import tech.testsys.infra.database.internal.jpa.entity.task.TestDiagnosticResultJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.CompositeJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository of completed polygon results.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TestDiagnosticResultJpaEntityRepository :
    CompositeJpaEntityRepository<TestDiagnosticResultJpaEntity, TestDiagnosticResultId> {
    /**
     * Finds the saved progress of a request.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestId(requestId: Long): List<TestDiagnosticResultJpaEntity>

    /**
     * Finds the saved progress of any of the requests [requestIds] in one query.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByIdRequestIdIn(requestIds: Collection<Long>): List<TestDiagnosticResultJpaEntity>

    /**
     * Deletes the saved progress of the request [requestId] in one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TestDiagnosticResultJpaEntity e where e.id.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}

/**
 * Spring Data repository of polygon diagnostic messages.
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface DiagnosticReportJpaEntityRepository : SnowflakeJpaEntityRepository<DiagnosticReportJpaEntity> {
    /**
     * Finds a polygon's messages in their original order.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByRequestIdAndTestIdOrderByPositionAsc(requestId: Long, testId: Long): List<DiagnosticReportJpaEntity>

    /**
     * Finds the messages of every polygon of any of the requests [requestIds] in one query, ordered by request, polygon
     * and original position.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByRequestIdInOrderByRequestIdAscTestIdAscPositionAsc(requestIds: Collection<Long>): List<DiagnosticReportJpaEntity>

    /**
     * Deletes the messages of every polygon of the request [requestId] in one statement.
     *
     * @since %CURRENT_VERSION%
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DiagnosticReportJpaEntity e where e.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}
