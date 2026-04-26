package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.ExerciseJpaEntity

/**
 * Spring Data repository for [ExerciseJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface ExerciseJpaEntityRepository : SequenceJpaEntityRepository<ExerciseJpaEntity>
