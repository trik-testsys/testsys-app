package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.IdClass
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import tech.testsys.infra.database.jpa.entity.JpaEntity

enum class TrikSupportedLanguageJpaEnum {
    PYTHON,
    JAVA_SCRIPT,
    VISUAL_LANGUAGE
}

@Entity
@Table(name = "t_judgment_order")
class JudgmentOrderJpaEntity(
    @Column(name = "judge_id")
    val judgeId: Long,

    @Column(name = "verdict_id")
    val verdictId: Long,
) : JpaEntity()

@MappedSuperclass
abstract class FileJpaEntity(
    @Column(name = "uploaded_filename")
    val uploadedFilename: String,
) : JpaEntity()

@Entity
@Table(name = "t_test")
class TestJpaEntity(
    uploadedFilename: String,
    @Column(name = "index")
    val index: Int,

    @Column(name = "root_id")
    val rootId: Long,

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

@Entity
@Table(name = "t_exercise")
class ExerciseJpaEntity(
    uploadedFilename: String,
    @Column(name = "index")
    val index: Int,

    @Column(name = "root_id")
    val rootId: Long,

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

@Entity
@Table(name = "t_solution")
class SolutionJpaEntity(
    uploadedFilename: String,

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    val language: TrikSupportedLanguageJpaEnum
) : FileJpaEntity(uploadedFilename)

//@IdClass
//class TestToTaskId(
//
//)


@Entity
@Table(name = "t_tests_to_tasks")
class TestsToTasksJpaEntity(

) : JpaEntity()

@Entity
@Table(name = "t_task")
class TaskJpaEntity(
    @Column(name = "owner_id")
    val ownerId: Long,

    @Column(name = "name")
    val name: String,

    @Column(name = "description")
    val description: String,

    @Column(name = "exercise_id")
    val exerciseId: Long

    // TODO: developerSolutions

) : JpaEntity()
