package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of an [Exercise].
 *
 * @since 1.0.0-beta.1
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
 * @property file the uploaded TRIK Studio program file; fixed on creation; update fails if it differs.
 * @property language the programming language of the exercise program; fixed on creation; update fails if it differs.
 * @property versionBucket the version chain shared by all versions of the same logical exercise;
 *   fixed on creation and ignored on update.
 * @since 1.0.0-beta.1
 */
class ExerciseData(
    val name: String,
    val description: String,
    val file: FileData,
    val language: TrikSupportedLanguage,
    val versionBucket: VersionBucket,
)

/**
 * A TRIK Studio program with a locked world model that solvers start from.
 *
 * @property data the data of the exercise.
 * @since 1.0.0-beta.1
 */
class Exercise(
    id: ExerciseId,
    createdAt: Instant,
    val data: ExerciseData,
) : DomainEntity<ExerciseId>(id, createdAt)
