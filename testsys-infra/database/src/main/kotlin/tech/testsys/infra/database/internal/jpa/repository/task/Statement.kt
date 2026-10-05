package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.StatementJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.util.UUID

/**
 * Spring Data repository for [StatementJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface StatementJpaEntityRepository : SnowflakeJpaEntityRepository<StatementJpaEntity> {

    /**
     * Finds the latest version in [versionBucket] by creation time and then id, both descending.
     *
     * @since %CURRENT_VERSION%
     */
    fun findFirstByVersionBucketOrderByCreatedAtDescIdDesc(versionBucket: UUID): StatementJpaEntity?

    /**
     * Finds versions in [versionBucket] without loading file contents.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByVersionBucket(versionBucket: UUID): List<StatementJpaEntity>

    /**
     * Checks whether [versionBucket] contains a version, without loading file contents.
     *
     * @since %CURRENT_VERSION%
     */
    fun existsByVersionBucket(versionBucket: UUID): Boolean
}
