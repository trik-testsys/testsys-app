package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.EntityVersion
import java.time.Instant
import java.util.UUID

/**
 * Identifier of an [Exercise].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class ExerciseId(
    override val value: Long,
) : DomainId

/**
 * Data of an [Exercise].
 *
 * @property name the name of the exercise.
 * @property description the description of the exercise.
 * @property file the uploaded TRIK Studio program file.
 * @property language the programming language of the exercise program.
 * @property versionBucket the UUID shared by all versions of the same logical exercise.
 * @since %CURRENT_VERSION%
 */
class ExerciseData(
    val name: String,
    val description: String,
    val file: FileData,
    val language: TrikSupportedLanguage,
    val versionBucket: UUID,
)

/**
 * A TRIK Studio program with a locked world model that solvers start from.
 *
 * @property data the data of the exercise.
 * @since %CURRENT_VERSION%
 */
class Exercise(
    id: ExerciseId,
    createdAt: Instant,
    version: EntityVersion,
    val data: ExerciseData,
) : DomainEntity<ExerciseId>(id, createdAt, version)
