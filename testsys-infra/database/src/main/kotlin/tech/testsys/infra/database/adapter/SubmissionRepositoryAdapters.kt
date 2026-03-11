package tech.testsys.infra.database.adapter

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Repository
import tech.testsys.domain.contract.EntityRepository
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.LazyNullableEntity
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.jpa.entity.JudgmentOrderJpaEntity
import tech.testsys.infra.database.jpa.entity.SubmissionJpaEntity
import tech.testsys.infra.database.jpa.entity.VerdictJpaEntity
import tech.testsys.infra.database.jpa.spring.JudgmentOrderSpringRepository
import tech.testsys.infra.database.jpa.spring.SubmissionSpringRepository
import tech.testsys.infra.database.jpa.spring.VerdictSpringRepository

@Repository
class VerdictRepositoryAdapter(
    private val verdictRepo: VerdictSpringRepository,
) : EntityRepository<VerdictData, VerdictId, Verdict> {

    override fun load(field: LazyEntity<VerdictId, Verdict>): Verdict {
        val jpa: VerdictJpaEntity = verdictRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Verdict ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<VerdictId, Verdict>): Verdict? =
        verdictRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<VerdictId, Verdict>, pageSize: Int, page: Int): List<Verdict> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return verdictRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: VerdictData): Verdict {
        val jpa = VerdictJpaEntity(
            score = data.score,
            taskId = data.task.id.value,
            submissionId = data.submission.id.value,
        )
        val saved = verdictRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<VerdictData>): List<Verdict> = data.map { save(it) }

    private fun toDomain(jpa: VerdictJpaEntity): Verdict = Verdict(
        id = VerdictId(jpa.id),
        data = VerdictData(
            score = jpa.score,
            task = LazyEntity(TaskId(jpa.taskId)),
            submission = LazyEntity(SubmissionId(jpa.submissionId)),
        )
    )
}

@Repository
class SubmissionRepositoryAdapter(
    private val submissionRepo: SubmissionSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<SubmissionData, SubmissionId, Submission> {

    override fun load(field: LazyEntity<SubmissionId, Submission>): Submission {
        val jpa: SubmissionJpaEntity = submissionRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Submission ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<SubmissionId, Submission>): Submission? =
        submissionRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<SubmissionId, Submission>, pageSize: Int, page: Int): List<Submission> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return submissionRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: SubmissionData): Submission {
        val jpa = SubmissionJpaEntity(
            authorId = data.author.id.value,
            solutionId = data.solution.id.value,
            taskId = data.task.id.value,
            createdAt = data.createdAt,
            judgmentOrderIds = data.judgmentOrders.ids.map { it.value }.toMutableList(),
        )
        applyStatus(jpa, data.status)
        applyKind(jpa, data.kind)
        val saved = submissionRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<SubmissionData>): List<Submission> = data.map { save(it) }

    private fun applyStatus(jpa: SubmissionJpaEntity, status: SubmissionStatus) {
        when (status) {
            is SubmissionStatus.Queued -> {
                jpa.statusType = "QUEUED"
            }
            is SubmissionStatus.InProgress -> {
                jpa.statusType = "IN_PROGRESS"
            }
            is SubmissionStatus.Graded -> {
                jpa.statusType = "GRADED"
                when (val grade = status.grade) {
                    is GradingResult.Success -> {
                        jpa.gradingResultType = "SUCCESS"
                        jpa.verdictId = grade.verdict.id.value
                    }
                    is GradingResult.GradingError -> {
                        jpa.gradingResultType = "ERROR"
                        jpa.gradingErrorDescription = grade.description
                    }
                    is GradingResult.Timeout -> {
                        jpa.gradingResultType = "TIMEOUT"
                    }
                }
            }
        }
    }

    private fun applyKind(jpa: SubmissionJpaEntity, kind: SubmissionKind) {
        when (kind) {
            is SubmissionKind.DeveloperSolutionTest -> {
                jpa.kindType = "DEVELOPER_SOLUTION_TEST"
            }
            is SubmissionKind.Grading -> {
                jpa.kindType = "GRADING"
                jpa.contestId = kind.contest.id.value
            }
        }
    }

    private fun toDomain(jpa: SubmissionJpaEntity): Submission {
        val author = userEntityLoader.loadById(jpa.authorId) as MultipleRoleUser
        return Submission(
            id = SubmissionId(jpa.id),
            data = SubmissionData(
                author = author,
                solution = LazyEntity(SolutionId(jpa.solutionId)),
                task = LazyEntity(TaskId(jpa.taskId)),
                status = parseStatus(jpa),
                createdAt = jpa.createdAt,
                kind = parseKind(jpa),
                judgmentOrders = LazyEntityList(jpa.judgmentOrderIds.map { JudgmentOrderId(it) }),
            )
        )
    }

    private fun parseStatus(jpa: SubmissionJpaEntity): SubmissionStatus = when (jpa.statusType) {
        "QUEUED" -> SubmissionStatus.Queued
        "IN_PROGRESS" -> SubmissionStatus.InProgress
        "GRADED" -> SubmissionStatus.Graded(
            grade = when (jpa.gradingResultType) {
                "SUCCESS" -> GradingResult.Success(
                    verdict = LazyEntity(VerdictId(jpa.verdictId!!))
                )
                "ERROR" -> GradingResult.GradingError(
                    description = jpa.gradingErrorDescription!!
                )
                "TIMEOUT" -> GradingResult.Timeout
                else -> error("Unknown grading result type: ${jpa.gradingResultType}")
            }
        )
        else -> error("Unknown submission status type: ${jpa.statusType}")
    }

    private fun parseKind(jpa: SubmissionJpaEntity): SubmissionKind = when (jpa.kindType) {
        "DEVELOPER_SOLUTION_TEST" -> SubmissionKind.DeveloperSolutionTest
        "GRADING" -> SubmissionKind.Grading(
            contest = LazyEntity(ContestId(jpa.contestId!!))
        )
        else -> error("Unknown submission kind type: ${jpa.kindType}")
    }
}

@Repository
class JudgmentOrderRepositoryAdapter(
    private val judgmentOrderRepo: JudgmentOrderSpringRepository,
) : EntityRepository<JudgmentOrderData, JudgmentOrderId, JudgmentOrder> {

    override fun load(field: LazyEntity<JudgmentOrderId, JudgmentOrder>): JudgmentOrder {
        val jpa: JudgmentOrderJpaEntity = judgmentOrderRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("JudgmentOrder ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<JudgmentOrderId, JudgmentOrder>): JudgmentOrder? =
        judgmentOrderRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(
        list: LazyEntityList<JudgmentOrderId, JudgmentOrder>,
        pageSize: Int,
        page: Int,
    ): List<JudgmentOrder> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return judgmentOrderRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: JudgmentOrderData): JudgmentOrder {
        val jpa = JudgmentOrderJpaEntity(
            judgeId = data.judge.id.value,
            verdictId = data.verdict.id.value,
            createdAt = data.createdAt,
        )
        val saved = judgmentOrderRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<JudgmentOrderData>): List<JudgmentOrder> = data.map { save(it) }

    private fun toDomain(jpa: JudgmentOrderJpaEntity): JudgmentOrder = JudgmentOrder(
        id = JudgmentOrderId(jpa.id),
        data = JudgmentOrderData(
            createdAt = jpa.createdAt,
            judge = LazyEntity(UserId(jpa.judgeId)),
            verdict = LazyEntity(VerdictId(jpa.verdictId)),
        )
    )
}