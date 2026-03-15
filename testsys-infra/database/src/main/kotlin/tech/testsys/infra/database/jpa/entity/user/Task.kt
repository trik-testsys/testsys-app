package tech.testsys.infra.database.jpa.entity.user

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
 * JPA entity representing a judgment order domain entity.
 *
 * @see tech.testsys.domain.model.task.JudgmentOrder
 * @see tech.testsys.domain.model.task.JudgmentOrderData
 * @since %CURRENT_VERSION%
 */
@Entity
class JudgmentOrderJpaEntity(
    val judgeId: Long,
    val verdictId: Long,
) : JpaEntity()

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

@Embeddable
data class DeveloperSolutionToTaskId(
    val developerSolutionId: Long,
    val taskId: Long,
) : JpaCompositeId()

@Entity
class DeveloperSolutionToTaskJpaEntity(
    id: TestToTaskId,
) : JpaCompositeEntity<TestToTaskId>(id)

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
    val trikStudioVersionName: String,
) : JpaEntity()

@Entity
class DeveloperSolutionJpaEntity(
    val solutionId: Long,
    val expectedVerdictId: Long,
) : JpaEntity()
