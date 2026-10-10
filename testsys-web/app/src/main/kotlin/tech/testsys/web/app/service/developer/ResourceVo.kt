package tech.testsys.web.app.service.developer

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import java.time.Instant

/**
 * Resource version data for pages; files are represented by their names without content.
 *
 * @property id the identifier of the resource version.
 * @property createdAt the moment the version was created.
 * @property name the name of the resource.
 * @property description the description of the resource.
 * @property versionBucket the chain of versions the resource belongs to.
 * @since %CURRENT_VERSION%
 */
sealed interface ResourceVo {
    val id: DomainId
    val createdAt: Instant
    val name: String
    val description: String
    val versionBucket: VersionBucket
}

/**
 * Statement version for pages.
 *
 * @property fileName the uploaded name of the statement file.
 * @since %CURRENT_VERSION%
 */
data class StatementVo(
    override val id: StatementId,
    override val createdAt: Instant,
    override val name: String,
    override val description: String,
    override val versionBucket: VersionBucket,
    val fileName: String,
) : ResourceVo

/**
 * Exercise version for pages.
 *
 * @property fileName the uploaded name of the exercise file.
 * @property language the language of the exercise.
 * @since %CURRENT_VERSION%
 */
data class ExerciseVo(
    override val id: ExerciseId,
    override val createdAt: Instant,
    override val name: String,
    override val description: String,
    override val versionBucket: VersionBucket,
    val fileName: String,
    val language: TrikSupportedLanguage,
) : ResourceVo

/**
 * Test version for pages.
 *
 * @property fileName the uploaded name of the polygon file.
 * @since %CURRENT_VERSION%
 */
data class TestVo(
    override val id: TestId,
    override val createdAt: Instant,
    override val name: String,
    override val description: String,
    override val versionBucket: VersionBucket,
    val fileName: String,
) : ResourceVo

/**
 * Developer solution version for pages.
 *
 * @property solution the identifier of the solution.
 * @property expectedScore the score the solution is expected to get.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionVo(
    override val id: DeveloperSolutionId,
    override val createdAt: Instant,
    override val name: String,
    override val description: String,
    override val versionBucket: VersionBucket,
    val solution: SolutionId,
    val expectedScore: Score,
) : ResourceVo

/**
 * Solution of a developer solution version for pages; its file is represented by its name without content.
 *
 * @property id the identifier of the solution.
 * @property createdAt the moment the solution was uploaded.
 * @property fileName the uploaded name of the program file.
 * @property language the programming language of the program.
 * @since %CURRENT_VERSION%
 */
data class SolutionVo(val id: SolutionId, val createdAt: Instant, val fileName: String, val language: TrikSupportedLanguage)

internal fun Solution.toVo() = SolutionVo(
    id = id,
    createdAt = createdAt,
    fileName = data.file.uploadedFilename,
    language = data.language,
)

internal fun Statement.toVo() = StatementVo(
    id = id,
    createdAt = createdAt,
    name = data.name,
    description = data.description,
    versionBucket = data.versionBucket,
    fileName = data.file.uploadedFilename,
)

internal fun Exercise.toVo() = ExerciseVo(
    id = id,
    createdAt = createdAt,
    name = data.name,
    description = data.description,
    versionBucket = data.versionBucket,
    fileName = data.file.uploadedFilename,
    language = data.language,
)

internal fun Test.toVo() = TestVo(
    id = id,
    createdAt = createdAt,
    name = data.name,
    description = data.description,
    versionBucket = data.versionBucket,
    fileName = data.file.uploadedFilename,
)

internal fun DeveloperSolution.toVo() = DeveloperSolutionVo(
    id = id,
    createdAt = createdAt,
    name = data.name,
    description = data.description,
    versionBucket = data.versionBucket,
    solution = data.solution.id,
    expectedScore = data.expectedScore,
)

internal fun DomainEntity<*>.toResourceVo(): ResourceVo = when (this) {
    is Statement -> toVo()
    is Exercise -> toVo()
    is Test -> toVo()
    is DeveloperSolution -> toVo()
    else -> error("Unexpected resource ${this::class.qualifiedName} with id ${id.value}")
}
