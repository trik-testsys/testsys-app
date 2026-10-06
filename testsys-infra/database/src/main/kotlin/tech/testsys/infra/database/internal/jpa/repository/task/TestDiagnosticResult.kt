package tech.testsys.infra.database.internal.jpa.repository.task

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
}
