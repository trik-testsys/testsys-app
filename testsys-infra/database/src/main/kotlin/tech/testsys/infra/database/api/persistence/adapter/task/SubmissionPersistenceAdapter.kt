package tech.testsys.infra.database.api.persistence.adapter.task

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
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
 * Judgment order ids are projected from the judgment order table on read,
 * and the kind of the author id is resolved from the user row; the TRIK Studio version must already be registered by tag.
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

    @Transactional
    override fun save(data: SubmissionData): Submission {
        val trikStudioVersionId = trikStudioVersionJpaEntityRepository.findIdByTagOrError(data.trikStudioVersion.version)
        val jpaEntity = SubmissionMapping.toJpaEntity(data, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = SubmissionMapping.toDomain(
            jpaEntity = savedJpaEntity,
            authorId = resolveAuthorId(savedJpaEntity.authorId),
            trikStudioVersion = data.trikStudioVersion,
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

    override fun assemble(jpaEntity: SubmissionJpaEntity): Submission {
        val submissionId = jpaEntity.requireId()
        val judgmentOrderIds = judgmentOrderJpaEntityRepository.findAllBySubmissionId(submissionId)
            .map { JudgmentOrderId(it.requireId()) }

        val tag = trikStudioVersionJpaEntityRepository.findByIdOrError(jpaEntity.trikStudioVersionId).tag
        val domainEntity = SubmissionMapping.toDomain(
            jpaEntity = jpaEntity,
            authorId = resolveAuthorId(jpaEntity.authorId),
            trikStudioVersion = TrikStudioVersion(tag),
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
