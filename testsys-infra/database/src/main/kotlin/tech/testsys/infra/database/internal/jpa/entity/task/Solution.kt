package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.SequenceJpaEntity

/**
 * JPA entity representing a participant solution domain entity.
 *
 * Submitted by a participant during a contest or as practice; the runtime
 * language of the source is captured by [language].
 *
 * @see tech.testsys.domain.model.task.Solution
 * @see tech.testsys.domain.model.task.SolutionData
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class SolutionJpaEntity(
    val fileDataId: Long,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
    id: Long? = null,
) : SequenceJpaEntity(id)
