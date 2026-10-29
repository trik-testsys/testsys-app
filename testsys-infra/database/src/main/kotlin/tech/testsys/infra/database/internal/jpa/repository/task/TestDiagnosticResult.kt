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
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface TestDiagnosticResultJpaEntityRepository :
    CompositeJpaEntityRepository<TestDiagnosticResultJpaEntity, TestDiagnosticResultId> {
    /**
     * Finds the saved progress of a request.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByIdRequestId(requestId: Long): List<TestDiagnosticResultJpaEntity>

    /**
     * Finds the saved progress of any of the requests [requestIds] in one query.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByIdRequestIdIn(requestIds: Collection<Long>): List<TestDiagnosticResultJpaEntity>

    /**
     * Deletes the saved progress of the request [requestId] in one statement.
     *
     * @since 1.0.0-beta.1
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TestDiagnosticResultJpaEntity e where e.id.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}

/**
 * Spring Data repository of polygon diagnostic messages.
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface DiagnosticReportJpaEntityRepository : SnowflakeJpaEntityRepository<DiagnosticReportJpaEntity> {
    /**
     * Finds a polygon's messages in their original order.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByRequestIdAndTestIdOrderByPositionAsc(requestId: Long, testId: Long): List<DiagnosticReportJpaEntity>

    /**
     * Finds the messages of every polygon of any of the requests [requestIds] in one query, ordered by request, polygon
     * and original position.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByRequestIdInOrderByRequestIdAscTestIdAscPositionAsc(requestIds: Collection<Long>): List<DiagnosticReportJpaEntity>

    /**
     * Deletes the messages of every polygon of the request [requestId] in one statement.
     *
     * @since 1.0.0-beta.1
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DiagnosticReportJpaEntity e where e.requestId = :requestId")
    fun deleteAllByRequestId(@Param("requestId") requestId: Long)
}
