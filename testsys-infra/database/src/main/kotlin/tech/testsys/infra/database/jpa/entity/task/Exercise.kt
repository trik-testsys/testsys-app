package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.util.UUID

/**
 * JPA entity representing an exercise domain entity.
 *
 * An exercise is the executable scaffold delivered to participants together with
 * a [StatementJpaEntity]; its programming language is captured by [language].
 * Owned by the developer who authored it ([ownerId]).
 *
 * @see tech.testsys.domain.model.task.Exercise
 * @see tech.testsys.domain.model.task.ExerciseData
 * @since %CURRENT_VERSION%
 */
@Entity
class ExerciseJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
    val ownerId: Long,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket)
