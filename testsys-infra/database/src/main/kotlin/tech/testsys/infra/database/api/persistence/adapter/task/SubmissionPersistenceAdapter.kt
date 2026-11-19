package tech.testsys.infra.database.api.persistence.adapter.task

import jakarta.persistence.criteria.CommonAbstractCriteria
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.JudgeSubmissionFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.SubmissionCount
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.StudentToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.GradingResultJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.StudentDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.JudgmentOrderJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TestVerdictJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TrikStudioVersionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.SubmissionMapping
import tech.testsys.infra.database.internal.utils.findAllByIdOrError
import tech.testsys.infra.database.internal.utils.findIdByTagOrError
import tech.testsys.infra.database.internal.utils.findLinkedIds
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.toDomain
import org.springframework.data.domain.Sort as JpaSort

/** Grading results of submissions that have a final score and are available to judges. */
@OptIn(InternalDatabaseApi::class)
private val JUDGEABLE_RESULTS = listOf(GradingResultJpaEnum.SUCCESS, GradingResultJpaEnum.INVALID_SUBMISSION)

/**
 * Persistence adapter of [Submission] entities backed by [SubmissionJpaEntity].
 * Reads judgment order ids and author id kinds from their rows; a developer solution test requires a registered version.
 *
 * @since 1.0.0-beta.1
 */
@Component
@OptIn(InternalDatabaseApi::class)
class SubmissionPersistenceAdapter(
    jpaEntityRepository: SubmissionJpaEntityRepository,
    private val judgmentOrderJpaEntityRepository: JudgmentOrderJpaEntityRepository,
    private val testVerdictJpaEntityRepository: TestVerdictJpaEntityRepository,
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
            trikStudioVersionJpaEntityRepository.findIdByTagOrError(it)
        }
        val jpaEntity = SubmissionMapping.toJpaEntity(data, trikStudioVersionId)
        val savedJpaEntity = jpaEntityRepository.save(jpaEntity)

        val domainEntity = SubmissionMapping.toDomain(
            jpaEntity = savedJpaEntity,
            authorId = loadAuthorIds(listOf(savedJpaEntity.authorId)).getValue(savedJpaEntity.authorId),
            trikStudioVersion = trikStudioVersion,
            judgmentOrderIds = emptyList(),
        )
        return domainEntity
    }

    @Transactional
    override fun update(entity: Submission): Submission {
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            SubmissionMapping.toJpaEntity(entity, current)
        }

        return assemble(savedJpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findGradingByTaskId(taskId: TaskId): List<Submission> = assembleAll(
        submissionJpaEntityRepository.findAllByTaskIdAndKindOrderByIdAsc(taskId = taskId.value, kind = SubmissionKindJpaEnum.GRADING),
    )

    @Transactional(readOnly = true)
    override fun findGradingByContext(authorId: UserId, taskId: TaskId, contestId: ContestId): List<Submission> {
        val rows = submissionJpaEntityRepository.findAllByAuthorIdAndTaskIdAndKindAndGradingContestIdOrderByCreatedAtAscIdAsc(
            authorId = authorId.value,
            taskId = taskId.value,
            kind = SubmissionKindJpaEnum.GRADING,
            gradingContestId = contestId.value,
        )
        return assembleAll(rows)
    }

    @Transactional(readOnly = true)
    override fun findGradingByContest(authorId: UserId, contestId: ContestId, taskIds: Set<TaskId>): List<Submission> {
        if (taskIds.isEmpty()) return emptyList()

        val rows = submissionJpaEntityRepository.findAllByKindAndGradingContestIdAndAuthorIdInAndTaskIdIn(
            kind = SubmissionKindJpaEnum.GRADING,
            gradingContestId = contestId.value,
            authorIds = setOf(authorId.value),
            taskIds = taskIds.map { taskId -> taskId.value },
        ).sortedWith(compareBy<SubmissionJpaEntity> { submission -> submission.createdAt }.thenBy { submission -> submission.requireId() })
        return assembleAll(rows)
    }

    @Transactional(readOnly = true)
    override fun findContestResults(contestId: ContestId, authorIds: Set<UserId>, taskIds: Set<TaskId>): List<ContestTaskResult> {
        if (authorIds.isEmpty() || taskIds.isEmpty()) return emptyList()

        val authorsByRawId = authorIds.associateBy { authorId -> authorId.value }
        val submissions = submissionJpaEntityRepository.findAllByKindAndGradingContestIdAndAuthorIdInAndTaskIdIn(
            kind = SubmissionKindJpaEnum.GRADING,
            gradingContestId = contestId.value,
            authorIds = authorsByRawId.keys,
            taskIds = taskIds.map { taskId -> taskId.value },
        )
        if (submissions.isEmpty()) return emptyList()

        val scores = findSubmissionScores(submissions.filter { submission -> submission.hasScore() })
        return submissions
            .sortedWith(compareBy<SubmissionJpaEntity> { submission -> submission.authorId }.thenBy { submission -> submission.taskId })
            .groupBy { submission -> submission.authorId to submission.taskId }
            .map { (key, group) ->
                ContestTaskResult(
                    authorId = authorsByRawId.getValue(key.first),
                    taskId = TaskId(key.second),
                    bestScore = group.mapNotNull { submission -> scores[submission.requireId()] }.maxOrNull()?.let(::Score),
                    submissionCount = group.size,
                )
            }
    }

    @Transactional(readOnly = true)
    override fun countGradingByTask(contestId: ContestId): Map<TaskId, Long> = submissionJpaEntityRepository
        .countGradingByTask(contestId.value)
        .associate { count -> TaskId(count.taskId) to count.submissions }

    @Transactional(readOnly = true)
    override fun countGrading(authorIds: Set<UserId>, contestIds: Set<ContestId>): SubmissionCount {
        if (authorIds.isEmpty() || contestIds.isEmpty()) return SubmissionCount(submissions = 0, authors = 0)

        val count = submissionJpaEntityRepository.countGrading(
            authorIds = authorIds.map { authorId -> authorId.value },
            contestIds = contestIds.map { contestId -> contestId.value },
        )
        return SubmissionCount(submissions = count.submissions, authors = count.authors)
    }

    @Transactional(readOnly = true)
    override fun findGradingAuthorIds(authorIds: Set<UserId>, contestIds: Set<ContestId>): Set<UserId> {
        if (authorIds.isEmpty() || contestIds.isEmpty()) return emptySet()

        val authorsByRawId = authorIds.associateBy { authorId -> authorId.value }
        return submissionJpaEntityRepository
            .findGradingAuthorIds(authorIds = authorsByRawId.keys, contestIds = contestIds.map { contestId -> contestId.value })
            .mapTo(mutableSetOf()) { authorId -> authorsByRawId.getValue(authorId) }
    }

    @Transactional(readOnly = true)
    override fun findAvailableToJudge(pagination: Pagination, filter: JudgeSubmissionFilter): Page<Submission> {
        val jpaSort = JpaSort.by(
            pagination.sort.orders.map { order ->
                JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
            },
        )
        val pageable = PageRequest.of(pagination.page, pagination.size, jpaSort)
        val page = submissionJpaEntityRepository.findAll(availableToJudge(filter), pageable)
        return Page(
            content = assembleAll(page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    override fun assembleAll(rows: List<SubmissionJpaEntity>): List<Submission> {
        val judgmentOrderIds = findLinkedIds(
            ownerIds = rows.map { row -> row.requireId() },
            find = judgmentOrderJpaEntityRepository::findAllBySubmissionIdIn,
            ownerIdOf = { order -> order.submissionId },
            linkedIdOf = { order -> JudgmentOrderId(order.requireId()) },
        )
        val authorIds = loadAuthorIds(rows.map { row -> row.authorId })
        val versions = trikStudioVersionJpaEntityRepository.findAllByIdOrError(rows.mapNotNull(::trikStudioVersionIdOf))

        return rows.map { row ->
            SubmissionMapping.toDomain(
                jpaEntity = row,
                authorId = authorIds.getValue(row.authorId),
                trikStudioVersion = trikStudioVersionIdOf(row)?.let { versionId -> versions.getValue(versionId).toDomain() },
                judgmentOrderIds = judgmentOrderIds.getValue(row.requireId()),
            )
        }
    }

    /**
     * Maps each submission id graded successfully or as invalid to its result: the latest judgment order score, otherwise
     * the verdict total or 0 for an invalid submission. Judgment orders and the polygon outcomes of the remaining verdicts
     * are each read in one query.
     */
    private fun findSubmissionScores(submissions: List<SubmissionJpaEntity>): Map<Long, Int> {
        if (submissions.isEmpty()) return emptyMap()

        val issueOrder = compareBy<JudgmentOrderJpaEntity> { order -> order.createdAt }.thenBy { order -> order.requireId() }
        val judgmentScores = judgmentOrderJpaEntityRepository.findAllBySubmissionIdIn(submissions.map { it.requireId() })
            .groupBy { order -> order.submissionId }
            .mapValues { (_, orders) -> orders.maxWith(issueOrder).score }
        val unjudged = submissions.filter { submission -> submission.requireId() !in judgmentScores }
        val invalidScores = unjudged
            .filter { submission -> submission.gradingResult == GradingResultJpaEnum.INVALID_SUBMISSION }
            .associate { submission -> submission.requireId() to 0 }
        val verdictIdsBySubmission = unjudged
            .filter { submission -> submission.gradingResult == GradingResultJpaEnum.SUCCESS }
            .associate { submission ->
                submission.requireId() to checkNotNull(submission.gradingVerdictId) {
                    "Submission ${submission.requireId()} is graded successfully but has no verdict id"
                }
            }
        val verdictTotals = if (verdictIdsBySubmission.isEmpty()) {
            emptyMap()
        } else {
            testVerdictJpaEntityRepository.findAllByVerdictIdInOrderByVerdictIdAscTestIdAsc(verdictIdsBySubmission.values.toList())
                .groupBy { outcome -> outcome.verdictId }
                .mapValues { (_, outcomes) -> outcomes.sumOf { outcome -> outcome.score } }
        }
        val verdictScores = verdictIdsBySubmission.mapValues { (submissionId, verdictId) ->
            checkNotNull(verdictTotals[verdictId]) { "Verdict $verdictId of submission $submissionId has no test outcomes" }
        }
        return judgmentScores + verdictScores + invalidScores
    }

    private fun SubmissionJpaEntity.hasScore(): Boolean = status == SubmissionStatusJpaEnum.GRADED &&
        gradingResult in JUDGEABLE_RESULTS

    /**
     * Selects grading submissions graded successfully or as invalid of current students or participants; the author,
     * submission, class and competition conditions are added only for the values set in [filter]. A group filter
     * requires current author membership and contest assignment to the same group.
     */
    private fun availableToJudge(filter: JudgeSubmissionFilter) = Specification<SubmissionJpaEntity> { submission, query, builder ->
        val authorId = submission.get<Long>("authorId")
        val contestId = submission.get<Long>("gradingContestId")
        val studentAuthor = builder.existsRow(
            parent = query,
            entity = StudentDataJpaEntity::class.java,
            selected = { student -> student.get("userId") },
            conditions = { student -> listOf(builder.equal(student.get<Long>("userId"), authorId)) },
        )
        val participantAuthor = builder.existsRow(
            parent = query,
            entity = ParticipantDataJpaEntity::class.java,
            selected = { participant -> participant.get("userId") },
            conditions = { participant -> listOf(builder.equal(participant.get<Long>("userId"), authorId)) },
        )
        val conditions = mutableListOf(
            builder.equal(submission.get<SubmissionKindJpaEnum>("kind"), SubmissionKindJpaEnum.GRADING),
            builder.equal(submission.get<SubmissionStatusJpaEnum>("status"), SubmissionStatusJpaEnum.GRADED),
            submission.get<GradingResultJpaEnum>("gradingResult").`in`(JUDGEABLE_RESULTS),
            builder.or(studentAuthor, participantAuthor),
        )
        filter.authorId?.let { author -> conditions += builder.equal(authorId, author.value) }
        filter.submissionId?.let { selected -> conditions += builder.equal(submission.get<Long>("id"), selected.value) }
        filter.classId?.let { selected ->
            conditions += builder.existsRow(
                parent = query,
                entity = StudentToClassJpaEntity::class.java,
                selected = { membership -> membership.idOf("studentId") },
                conditions = { membership ->
                    listOf(
                        builder.equal(membership.idOf("studentId"), authorId),
                        builder.equal(membership.idOf("classId"), selected.value),
                    )
                },
            )
            conditions += builder.existsRow(
                parent = query,
                entity = ContestToClassJpaEntity::class.java,
                selected = { assignment -> assignment.idOf("contestId") },
                conditions = { assignment ->
                    listOf(
                        builder.equal(assignment.idOf("contestId"), contestId),
                        builder.equal(assignment.idOf("classId"), selected.value),
                    )
                },
            )
        }
        filter.competitionId?.let { selected ->
            conditions += builder.existsRow(
                parent = query,
                entity = ParticipantDataJpaEntity::class.java,
                selected = { participant -> participant.get("userId") },
                conditions = { participant ->
                    listOf(
                        builder.equal(participant.get<Long>("userId"), authorId),
                        builder.equal(participant.get<Long>("competitionId"), selected.value),
                    )
                },
            )
            conditions += builder.existsRow(
                parent = query,
                entity = ContestToCompetitionJpaEntity::class.java,
                selected = { assignment -> assignment.idOf("contestId") },
                conditions = { assignment ->
                    listOf(
                        builder.equal(assignment.idOf("contestId"), contestId),
                        builder.equal(assignment.idOf("competitionId"), selected.value),
                    )
                },
            )
        }

        builder.and(*conditions.toTypedArray())
    }

    /**
     * Builds `exists (select [selected] from [entity] where [conditions])` as a subquery of [parent].
     */
    private fun <E : Any> CriteriaBuilder.existsRow(
        parent: CommonAbstractCriteria,
        entity: Class<E>,
        selected: (Root<E>) -> Path<Long>,
        conditions: (Root<E>) -> List<Predicate>,
    ): Predicate {
        val subquery = parent.subquery(Long::class.java)
        val row = subquery.from(entity)
        subquery.select(selected(row)).where(*conditions(row).toTypedArray())
        return exists(subquery)
    }

    private fun Root<*>.idOf(field: String): Path<Long> = get<Any>("id").get(field)

    private fun trikStudioVersionIdOf(jpaEntity: SubmissionJpaEntity): Long? = when (jpaEntity.kind) {
        SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST -> requireNotNull(jpaEntity.trikStudioVersionId) {
            "Submission ${jpaEntity.requireId()} has kind=DEVELOPER_SOLUTION_TEST but trikStudioVersionId is null"
        }
        SubmissionKindJpaEnum.GRADING -> null
    }

    /**
     * Maps each of the raw [authorIds] to the user id of its kind, reading the user rows in one query.
     */
    private fun loadAuthorIds(authorIds: List<Long>): Map<Long, UserId> =
        userJpaEntityRepository.findAllByIdOrError(authorIds).mapValues { (authorId, userJpaEntity) ->
            when (userJpaEntity.type) {
                UserTypeJpaEnum.MULTIPLE_ROLE -> MultipleRoleUserId(authorId)
                UserTypeJpaEnum.SINGLE_ROLE -> SingleRoleUserId(authorId)
            }
        }
}
