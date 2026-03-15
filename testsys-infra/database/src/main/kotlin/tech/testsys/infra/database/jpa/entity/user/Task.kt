package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * @since %CURRENT_VERSION%
 */
enum class TrikSupportedLanguageJpaEnum {
    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE
}

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class JudgmentOrderJpaEntity(
    val judgeId: Long,
    val verdictId: Long,
) : JpaEntity()

/**
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class FileJpaEntity(
    val uploadedFilename: String,
) : JpaEntity()

/**
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
 * @since %CURRENT_VERSION%
 */
@Entity
class SolutionJpaEntity(
    uploadedFilename: String,
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

@IdClass
class TestToTaskId(

)

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class TestsToTasksJpaEntity(

) : JpaEntity()

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class TaskJpaEntity(
    val ownerId: Long,
    val name: String,
    val description: String,
    val exerciseId: Long

    // TODO: developerSolutions

) : JpaEntity()