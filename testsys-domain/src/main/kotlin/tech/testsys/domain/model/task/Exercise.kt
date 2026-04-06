package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

@JvmInline
value class ExerciseId(
    override val value: Long,
) : DomainId

class ExerciseData(
    val file: FileData,
    val language: TrikSupportedLanguage,
)

class Exercise(
    id: ExerciseId,
    createdAt: Instant,
    val versionData: VersionData<ExerciseId, Exercise>,
    val data: ExerciseData,
) : DomainEntity<ExerciseId>(id, createdAt)