package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.util.UUID

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
class SolutionJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)
