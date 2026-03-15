package tech.testsys.infra.database.jpa.entity.task

import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.JpaCompositeEntity
import tech.testsys.infra.database.jpa.entity.JpaCompositeId
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * Supported programming languages for TRIK Studio tasks.
 *
 * @see tech.testsys.domain.model.task.TrikSupportedLanguage
 * @since %CURRENT_VERSION%
 */
enum class TrikSupportedLanguageJpaEnum {
    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE;
}

/**
 * Base JPA entity for file-based domain entities.
 *
 * @see tech.testsys.domain.model.task.FileData
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class FileJpaEntity(
    val uploadedFilename: String,
) : JpaEntity()

/**
 * JPA entity representing a test domain entity.
 *
 * @see tech.testsys.domain.model.task.Test
 * @see tech.testsys.domain.model.task.TestData
 * @since %CURRENT_VERSION%
 */
@Entity
class TestJpaEntity(
    uploadedFilename: String,
    val index: Int,
    val rootId: Long,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

/**
 * JPA entity representing an exercise domain entity.
 *
 * @see tech.testsys.domain.model.task.Exercise
 * @see tech.testsys.domain.model.task.ExerciseData
 * @since %CURRENT_VERSION%
 */
@Entity
class ExerciseJpaEntity(
    uploadedFilename: String,
    val index: Int,
    val rootId: Long,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

/**
 * JPA entity representing a solution domain entity.
 *
 * @see tech.testsys.domain.model.task.Solution
 * @see tech.testsys.domain.model.task.SolutionData
 * @since %CURRENT_VERSION%
 */
@Entity
class SolutionJpaEntity(
    uploadedFilename: String,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

/**
 * Composite primary key for [TestToTaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class TestToTaskId(
    val testId: Long,
    val taskId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a test to task association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class TestToTaskJpaEntity(
    id: TestToTaskId,
) : JpaCompositeEntity<TestToTaskId>(id)

/**
 * Composite primary key for [DeveloperSolutionToTaskJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Embeddable
data class DeveloperSolutionToTaskId(
    val developerSolutionId: Long,
    val taskId: Long,
) : JpaCompositeId()

/**
 * JPA entity representing a developer solution to task association domain entity.
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class DeveloperSolutionToTaskJpaEntity(
    id: DeveloperSolutionToTaskId,
) : JpaCompositeEntity<DeveloperSolutionToTaskId>(id)

/**
 * JPA entity representing a task domain entity.
 *
 * @see tech.testsys.domain.model.task.Task
 * @see tech.testsys.domain.model.task.TaskData
 * @since %CURRENT_VERSION%
 */
@Entity
class TaskJpaEntity(
    val ownerId: Long,
    val name: String,
    val description: String,
    val exerciseId: Long,
    val trikStudioVersionImage: String,
    val trikStudioVersionTag: String,
) : JpaEntity()

/**
 * JPA entity representing a developer solution domain entity.
 *
 * @see tech.testsys.domain.model.task.DeveloperSolution
 * @see tech.testsys.domain.model.task.DeveloperSolutionData
 * @since %CURRENT_VERSION%
 */
@Entity
class DeveloperSolutionJpaEntity(
    val solutionId: Long,
    val expectedVerdictId: Long,
) : JpaEntity()
