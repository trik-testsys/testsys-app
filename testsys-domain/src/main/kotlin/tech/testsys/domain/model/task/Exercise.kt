package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant
import java.util.UUID

@JvmInline
value class ExerciseId(
    override val value: Long,
) : DomainId

class ExerciseData(
    val name: String,
    val description: String,
    val file: FileData,
    val language: TrikSupportedLanguage,
    val versionBucket: UUID,
)

class Exercise(
    id: ExerciseId,
    createdAt: Instant,
    val data: ExerciseData,
) : DomainEntity<ExerciseId>(id, createdAt)
