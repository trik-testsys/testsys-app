package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.TestJpaEntity

/**
 * Spring Data repository for [TestJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface TestJpaEntityRepository : SequenceJpaEntityRepository<TestJpaEntity>
