package tech.testsys.infra.database.internal.mapping.task

import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.data
import tech.testsys.domain.builder.util.chooser.GradingSubmissionStatusBuilder
import tech.testsys.domain.builder.util.chooser.SubmissionKindChooser
import tech.testsys.domain.builder.util.chooser.SubmissionStatusChooser
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.GradingResultJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [Submission] and [SubmissionJpaEntity]; the sealed status and kind are flattened into nullable columns.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object SubmissionMapping : EntityMapping<Submission, SubmissionJpaEntity> {

    /**
     * Assembles a [Submission] from [jpaEntity] and [judgmentOrderIds]; fails on inconsistent status or kind columns.
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: SubmissionJpaEntity, judgmentOrderIds: List<JudgmentOrderId>) = submission {
        populateFields(jpaEntity)
        data {
            author(jpaEntity.authorId)
            solution(jpaEntity.solutionId)
            task(jpaEntity.taskId)

            status.decodeStatus(jpaEntity)
            kind.decodeKind(jpaEntity)

            judgmentOrders = judgmentOrderIds.toMutableList()
        }
    }

    /**
     * Creates a new [SubmissionJpaEntity] row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: SubmissionData): SubmissionJpaEntity {
        val statusEncoded = encodeStatus(data.status)
        val kindEncoded = encodeKind(data.kind)

        return SubmissionJpaEntity(
            authorId = data.author.id.value,
            solutionId = data.solution.id.value,
            taskId = data.task.id.value,
            status = statusEncoded.status,
            gradingResult = statusEncoded.gradingResult,
            gradingVerdictId = statusEncoded.gradingVerdictId,
            gradingErrorDescription = statusEncoded.gradingErrorDescription,
            kind = kindEncoded.kind,
            gradingContestId = kindEncoded.gradingContestId,
        )
    }

    /**
     * Creates the [SubmissionJpaEntity] row replacing [current] from [entity], keeping `createdAt` and `version`.
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(entity: Submission, current: SubmissionJpaEntity): SubmissionJpaEntity {
        val statusEncoded = encodeStatus(entity.data.status)
        val kindEncoded = encodeKind(entity.data.kind)

        return SubmissionJpaEntity(
            authorId = entity.data.author.id.value,
            solutionId = entity.data.solution.id.value,
            taskId = entity.data.task.id.value,
            status = statusEncoded.status,
            gradingResult = statusEncoded.gradingResult,
            gradingVerdictId = statusEncoded.gradingVerdictId,
            gradingErrorDescription = statusEncoded.gradingErrorDescription,
            kind = kindEncoded.kind,
            gradingContestId = kindEncoded.gradingContestId,
            id = entity.id.value,
        ).also {
            it.createdAt = current.createdAt
            it.version = entity.version.value
        }
    }

    private fun SubmissionStatusChooser.decodeStatus(jpaEntity: SubmissionJpaEntity) = when (jpaEntity.status) {
        SubmissionStatusJpaEnum.QUEUED -> {
            jpaEntity.requireNullPayload(SubmissionStatusJpaEnum.QUEUED)
            queued()
        }
        SubmissionStatusJpaEnum.IN_PROGRESS -> {
            jpaEntity.requireNullPayload(SubmissionStatusJpaEnum.IN_PROGRESS)
            inProgress()
        }
        SubmissionStatusJpaEnum.GRADED -> {
            val result = jpaEntity.requireGradingResult()

            graded {
                decodeGradingResult(jpaEntity, result)
            }
        }
    }

    private fun GradingSubmissionStatusBuilder.decodeGradingResult(jpaEntity: SubmissionJpaEntity, result: GradingResultJpaEnum) {
        when (result) {
            GradingResultJpaEnum.SUCCESS -> {
                val verdictId = jpaEntity.gradingVerdictId
                    ?: error("Submission ${jpaEntity.id} has gradingResult=SUCCESS but gradingVerdictId is null")
                require(jpaEntity.gradingErrorDescription == null) {
                    "Submission ${jpaEntity.id} has gradingResult=SUCCESS but gradingErrorDescription is set"
                }

                status.success {
                    verdict(verdictId)
                }
            }
            GradingResultJpaEnum.GRADING_ERROR -> {
                val description = jpaEntity.gradingErrorDescription
                    ?: error("Submission ${jpaEntity.id} has gradingResult=GRADING_ERROR but gradingErrorDescription is null")
                require(jpaEntity.gradingVerdictId == null) {
                    "Submission ${jpaEntity.id} has gradingResult=GRADING_ERROR but gradingVerdictId is set"
                }

                status.error {
                    this.description = description
                }
            }
            GradingResultJpaEnum.TIMEOUT -> {
                require(jpaEntity.gradingVerdictId == null && jpaEntity.gradingErrorDescription == null) {
                    "Submission ${jpaEntity.id} has gradingResult=TIMEOUT but verdict/error payload is set"
                }

                status.timeout()
            }
        }
    }

    private fun SubmissionKindChooser.decodeKind(jpaEntity: SubmissionJpaEntity) = when (jpaEntity.kind) {
        SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST -> {
            require(jpaEntity.gradingContestId == null) {
                "Submission ${jpaEntity.id} has kind=DEVELOPER_SOLUTION_TEST but gradingContestId is set"
            }

            developerSolutionTest()
        }
        SubmissionKindJpaEnum.GRADING -> {
            val contestId = jpaEntity.gradingContestId
                ?: error("Submission ${jpaEntity.id} has kind=GRADING but gradingContestId is null")

            grading {
                contest(contestId)
            }
        }
    }

    private fun SubmissionJpaEntity.requireNullPayload(status: SubmissionStatusJpaEnum) {
        require(
            gradingResult == null &&
                gradingVerdictId == null &&
                gradingErrorDescription == null,
        ) {
            "Submission $id has status=$status but grading payload is set"
        }
    }

    private fun SubmissionJpaEntity.requireGradingResult() = requireNotNull(gradingResult) {
        "Submission $id has status=${SubmissionStatusJpaEnum.GRADED} but gradingResult is null"
    }

    private data class StatusFlat(
        val status: SubmissionStatusJpaEnum,
        val gradingResult: GradingResultJpaEnum?,
        val gradingVerdictId: Long?,
        val gradingErrorDescription: String?,
    )

    private fun encodeStatus(status: SubmissionStatus): StatusFlat = when (status) {
        SubmissionStatus.Queued -> StatusFlat(SubmissionStatusJpaEnum.QUEUED, null, null, null)
        SubmissionStatus.InProgress -> StatusFlat(SubmissionStatusJpaEnum.IN_PROGRESS, null, null, null)
        is SubmissionStatus.Graded -> when (val grade = status.grade) {
            is GradingResult.Success -> StatusFlat(
                status = SubmissionStatusJpaEnum.GRADED,
                gradingResult = GradingResultJpaEnum.SUCCESS,
                gradingVerdictId = grade.verdict.id.value,
                gradingErrorDescription = null,
            )
            is GradingResult.GradingError -> StatusFlat(
                status = SubmissionStatusJpaEnum.GRADED,
                gradingResult = GradingResultJpaEnum.GRADING_ERROR,
                gradingVerdictId = null,
                gradingErrorDescription = grade.description,
            )
            GradingResult.Timeout -> StatusFlat(
                status = SubmissionStatusJpaEnum.GRADED,
                gradingResult = GradingResultJpaEnum.TIMEOUT,
                gradingVerdictId = null,
                gradingErrorDescription = null,
            )
        }
    }

    private data class KindFlat(
        val kind: SubmissionKindJpaEnum,
        val gradingContestId: Long?,
    )

    private fun encodeKind(kind: SubmissionKind): KindFlat = when (kind) {
        SubmissionKind.DeveloperSolutionTest -> KindFlat(SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST, null)
        is SubmissionKind.Grading -> KindFlat(SubmissionKindJpaEnum.GRADING, kind.contest.id.value)
    }
}
