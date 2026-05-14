package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.task.CommitedTaskContent
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.WipTaskContent
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.DeveloperSolutionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TestToTaskContentJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.TrikStudioVersionToTaskContentJpaEntity
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Mapper for a single [TaskContentJpaEntity] revision and its association rows.
 *
 * `TaskContent`'s sealed variants (`New` / `Uncommited` / `Committed`) are
 * composed at the [tech.testsys.infra.database.internal.mapping.task.TaskMapping] level; this object handles a single revision
 * in isolation.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object TaskContentMapping {

    fun toWipDomain(
        jpaEntity: TaskContentJpaEntity,
        testIds: List<TestId>,
        developerSolutionIds: List<DeveloperSolutionId>,
        supportedVersions: List<TrikStudioVersion>,
    ): WipTaskContent {
        jpaEntity.requireId()
        return WipTaskContent(
            tests = testIds.lazify(),
            exercise = jpaEntity.exerciseId?.let { LazyEntity(ExerciseId(it)) },
            statement = jpaEntity.statementId?.let { LazyEntity(StatementId(it)) },
            developerSolutions = developerSolutionIds.lazify(),
            supportedTrikStudioVersions = supportedVersions,
        )
    }

    fun toCommitedDomain(
        jpaEntity: TaskContentJpaEntity,
        testIds: List<TestId>,
        developerSolutionIds: List<DeveloperSolutionId>,
        supportedVersions: List<TrikStudioVersion>,
    ): CommitedTaskContent {
        val exerciseId = requireNotNull(jpaEntity.exerciseId) {
            "TaskContent ${jpaEntity.requireId()} marks committed payload but exerciseId is null"
        }
        val statementId = requireNotNull(jpaEntity.statementId) {
            "TaskContent ${jpaEntity.requireId()} marks committed payload but statementId is null"
        }
        return CommitedTaskContent(
            tests = testIds.lazify(),
            exercise = ExerciseId(exerciseId).lazify(),
            statement = StatementId(statementId).lazify(),
            developerSolutions = developerSolutionIds.lazify(),
            supportedTrikStudioVersions = supportedVersions,
        )
    }

    fun toJpaEntity(content: WipTaskContent) = TaskContentJpaEntity(
        exerciseId = content.exercise?.id?.value,
        statementId = content.statement?.id?.value,
    )

    fun toJpaEntity(content: CommitedTaskContent) = TaskContentJpaEntity(
        exerciseId = content.exercise.id.value,
        statementId = content.statement.id.value,
    )

    fun toTestAssociations(taskContentId: Long, testIds: List<TestId>) = testIds.map {
        TestToTaskContentJpaEntity(testId = it.value, taskContentId = taskContentId)
    }

    fun toDeveloperSolutionAssociations(taskContentId: Long, developerSolutionIds: List<DeveloperSolutionId>) = developerSolutionIds.map {
        DeveloperSolutionToTaskContentJpaEntity(developerSolutionId = it.value, taskContentId = taskContentId)
    }

    fun toTrikStudioVersionAssociations(taskContentId: Long, trikStudioVersionIds: List<Long>) = trikStudioVersionIds.map {
        TrikStudioVersionToTaskContentJpaEntity(trikStudioVersionId = it, taskContentId = taskContentId)
    }
}
