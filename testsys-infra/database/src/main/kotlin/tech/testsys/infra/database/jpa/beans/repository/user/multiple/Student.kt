package tech.testsys.infra.database.jpa.beans.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.multiple.StudentDataJpaEntity

/**
 * Spring Data repository for [StudentDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface StudentDataJpaEntityRepository : SequenceJpaEntityRepository<StudentDataJpaEntity>
