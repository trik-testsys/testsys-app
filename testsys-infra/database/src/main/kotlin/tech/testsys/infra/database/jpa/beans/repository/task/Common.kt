package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.FileDataJpaEntity
import tech.testsys.infra.database.jpa.entity.task.TrikStudioVersionJpaEntity

/**
 * Spring Data repository for [FileDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface FileDataJpaEntityRepository : SequenceJpaEntityRepository<FileDataJpaEntity>

/**
 * Spring Data repository for [TrikStudioVersionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface TrikStudioVersionJpaEntityRepository : SequenceJpaEntityRepository<TrikStudioVersionJpaEntity> {

    fun findByTag(tag: String): TrikStudioVersionJpaEntity?
}
