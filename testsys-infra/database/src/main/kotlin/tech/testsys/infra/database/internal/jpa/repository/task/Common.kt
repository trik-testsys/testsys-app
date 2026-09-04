package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [FileDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface FileDataJpaEntityRepository : SequenceJpaEntityRepository<FileDataJpaEntity>

/**
 * Spring Data repository for [TrikStudioVersionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface TrikStudioVersionJpaEntityRepository : SequenceJpaEntityRepository<TrikStudioVersionJpaEntity> {

    /**
     * Finds the version with [tag], or `null` if it is not stored.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByTag(tag: String): TrikStudioVersionJpaEntity?
}
