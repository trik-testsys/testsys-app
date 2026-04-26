package tech.testsys.infra.database.jpa.beans.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.multiple.JudgeDataJpaEntity

/**
 * Spring Data repository for [JudgeDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface JudgeDataJpaEntityRepository : SequenceJpaEntityRepository<JudgeDataJpaEntity>
