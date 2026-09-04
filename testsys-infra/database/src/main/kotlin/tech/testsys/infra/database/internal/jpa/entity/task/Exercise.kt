package tech.testsys.infra.database.internal.jpa.entity.task

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.UUID

/**
 * JPA entity of [tech.testsys.domain.model.task.Exercise].
 *
 * @property language programming language of the exercise.
 * @since %CURRENT_VERSION%
 */
@Entity
@InternalDatabaseApi
class ExerciseJpaEntity(
    name: String,
    description: String,
    fileDataId: Long,
    versionBucket: UUID,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageEnum,
    id: Long? = null,
) : ResourceJpaEntity(name, description, fileDataId, versionBucket, id)
