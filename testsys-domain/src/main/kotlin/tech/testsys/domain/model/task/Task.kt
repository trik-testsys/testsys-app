package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.user.MultipleRoleUser

sealed interface TrikSupportedLanguage {
    object Python: TrikSupportedLanguage
    object JavaScript: TrikSupportedLanguage
    object VisualLanguage: TrikSupportedLanguage
}

@JvmInline
value class DeveloperSolutionId(
    override val value: Long,
) : DomainId

data class DeveloperSolutionData(
    val solution: LazyEntity<SolutionId, Solution>,
    val expectedVerdict: LazyEntity<VerdictId, Verdict>,
)

class DeveloperSolution(
    id: DeveloperSolutionId,
    val data: DeveloperSolutionData,
) : DomainEntity<DeveloperSolutionId>(id)

@JvmInline
value class TaskId(
    override val value: Long,
) : DomainId

data class TrikStudioVersion(
    val image: String,
    val tag: String,
)

data class TaskData(
    val owner: MultipleRoleUser,
    val name: String,
    val description: String,
    val tests: LazyEntityList<TestId, Test>,
    val exercise: LazyEntity<ExerciseId, Exercise>,
    val developerSolutions: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
    val trikStudioVersion: TrikStudioVersion, // TODO: here?
)

class Task(
    id: TaskId,
    val data: TaskData,
) : DomainEntity<TaskId>(id)

@JvmInline
value class TestId(
    override val value: Long,
) : DomainId

class FileData(
    val uploadedFilename: String,
    val content: ByteArray,
)

data class VersionData<Id: DomainId, Entity: DomainEntity<Id>>(
    val root: LazyEntity<Id, Entity>,
    val index: Long,
)

class TestData(
    val file: FileData,
    val versionData: VersionData<TestId, Test>,
    val language: TrikSupportedLanguage,
)

// TODO: add data like timelimit, analysis data
class Test(
    id: TestId,
    val data: TestData,
) : DomainEntity<TestId>(id)

@JvmInline
value class SolutionId(
    override val value: Long,
) : DomainId

class SolutionData(
    val file: FileData,
    val language: TrikSupportedLanguage,
)

class Solution(
    id: SolutionId,
    val data: SolutionData,
) : DomainEntity<SolutionId>(id)

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
    val versionData: VersionData<ExerciseId, Exercise>,
    val data: ExerciseData
) : DomainEntity<ExerciseId>(id)

