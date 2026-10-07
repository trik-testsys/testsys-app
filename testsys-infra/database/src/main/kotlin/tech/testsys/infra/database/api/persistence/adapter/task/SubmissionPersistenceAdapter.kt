package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.task.JudgmentOrderJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.SubmissionMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.requireId

/**
 * Persistence adapter of [Submission] entities backed by [SubmissionJpaEntity].
 * Reads judgment order ids and author id kinds from their rows; a developer solution test requires a registered version.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class SubmissionPersistenceAdapter(
    jpaEntityRepository: SubmissionJpaEntityRepository,
    private val judgmentOrderJpaEntityRepository: JudgmentOrderJpaEntityRepository,
    private val userJpaEntityRepository: UserJpaEntityRepository,
    private val trikStudioVersionJpaEntityRepository: TrikStudioVersionJpaEntityRepository,
) : AbstractPersistenceAdapter<SubmissionData, SubmissionId, Submission, SubmissionJpaEntity>(jpaEntityRepository),
    SubmissionRepository {

    private val submissionJpaEntityRepository: SubmissionJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: SubmissionData): Submission {
        val trikStudioVersion = when (val kind = data.kind) {
            is SubmissionKind.DeveloperSolutionTest -> kind.trikStudioVersion
            is SubmissionKind.Grading -> null
        }
        val trikStudioVersionId = trikStudioVersion?.let {
            trikStudioVersionJpaEntityRepository.findIdByTagOrError(it.version)
        }
        val jpaEntity = SubmissionMapping.toJpaEntity(data, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = SubmissionMapping.toDomain(
            jpaEntity = savedJpaEntity,
            authorId = resolveAuthorId(savedJpaEntity.authorId),
            trikStudioVersion = trikStudioVersion,
            judgmentOrderIds = emptyList(),
        )
        return domainEntity
    }

    @Transactional
    override fun update(entity: Submission): Submission {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = SubmissionMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findGradingByTaskId(taskId: TaskId): List<Submission> = submissionJpaEntityRepository
        .findAllByTaskIdAndKindOrderByIdAsc(taskId = taskId.value, kind = SubmissionKindJpaEnum.GRADING)
        .map { jpaEntity -> assemble(jpaEntity) }

    override fun assemble(jpaEntity: SubmissionJpaEntity): Submission {
        val submissionId = jpaEntity.requireId()
        val judgmentOrderIds = judgmentOrderJpaEntityRepository.findAllBySubmissionId(submissionId)
            .map { JudgmentOrderId(it.requireId()) }

        val trikStudioVersion = when (jpaEntity.kind) {
            SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST -> {
                val versionId = requireNotNull(jpaEntity.trikStudioVersionId) {
                    "Submission $submissionId has kind=DEVELOPER_SOLUTION_TEST but trikStudioVersionId is null"
                }
                TrikStudioVersion(version = trikStudioVersionJpaEntityRepository.findByIdOrError(versionId).tag)
            }
            SubmissionKindJpaEnum.GRADING -> null
        }
        val domainEntity = SubmissionMapping.toDomain(
            jpaEntity = jpaEntity,
            authorId = resolveAuthorId(jpaEntity.authorId),
            trikStudioVersion = trikStudioVersion,
            judgmentOrderIds = judgmentOrderIds,
        )
        return domainEntity
    }

    private fun resolveAuthorId(authorId: Long): UserId {
        val userJpaEntity = userJpaEntityRepository.findByIdOrError(authorId)
        return when (userJpaEntity.type) {
            UserTypeJpaEnum.MULTIPLE_ROLE -> MultipleRoleUserId(authorId)
            UserTypeJpaEnum.SINGLE_ROLE -> SingleRoleUserId(authorId)
        }
    }
}
