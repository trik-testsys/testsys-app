package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.TestJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository
import java.util.UUID

/**
 * Spring Data repository for [TestJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface TestJpaEntityRepository : SnowflakeJpaEntityRepository<TestJpaEntity> {

    /**
     * Finds the latest version in [versionBucket] by creation time and then id, both descending.
     *
     * @since 1.0.0-beta.1
     */
    fun findFirstByVersionBucketOrderByCreatedAtDescIdDesc(versionBucket: UUID): TestJpaEntity?

    /**
     * Finds versions in [versionBucket] without loading file contents.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByVersionBucket(versionBucket: UUID): List<TestJpaEntity>

    /**
     * Checks whether [versionBucket] contains a version, without loading file contents.
     *
     * @since 1.0.0-beta.1
     */
    fun existsByVersionBucket(versionBucket: UUID): Boolean
}
