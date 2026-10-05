package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.LogsJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.RecordingJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestVerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [VerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface VerdictJpaEntityRepository : SnowflakeJpaEntityRepository<VerdictJpaEntity>

/**
 * Spring Data repository for [TestVerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TestVerdictJpaEntityRepository : SnowflakeJpaEntityRepository<TestVerdictJpaEntity> {

    /**
     * Finds the test outcomes of the verdict [verdictId], ordered by the id of the test.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByVerdictIdOrderByTestIdAsc(verdictId: Long): List<TestVerdictJpaEntity>
}

/**
 * Spring Data repository for [RecordingJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface RecordingJpaEntityRepository : SnowflakeJpaEntityRepository<RecordingJpaEntity>

/**
 * Spring Data repository for [LogsJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface LogsJpaEntityRepository : SnowflakeJpaEntityRepository<LogsJpaEntity>

/**
 * Spring Data repository for [SubmissionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface SubmissionJpaEntityRepository : SnowflakeJpaEntityRepository<SubmissionJpaEntity> {

    /**
     * Finds the submissions authored by the user [authorId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByAuthorId(authorId: Long): List<SubmissionJpaEntity>
}
