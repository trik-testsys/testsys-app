package tech.testsys.infra.database.api.persistence.adapter.task

import jakarta.persistence.criteria.CommonAbstractCriteria
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.VerdictFilter
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.ContestToCompetitionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.group.StudentToClassJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.GradingResultJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.VerdictJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.StudentDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.single.ParticipantDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.task.TestVerdictJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.VerdictJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.task.VerdictMapping
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.requireId
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Verdict] entities backed by [VerdictJpaEntity].
 * The outcome of every test run is stored in its own row and dropped on remove after the verdict version is
 * incremented; a verdict is fixed on creation, so [update] always fails.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class VerdictPersistenceAdapter(
    jpaEntityRepository: VerdictJpaEntityRepository,
    private val testVerdictJpaEntityRepository: TestVerdictJpaEntityRepository,
) : AbstractPersistenceAdapter<VerdictData, VerdictId, Verdict, VerdictJpaEntity>(jpaEntityRepository),
    VerdictRepository {

    private val verdictJpaEntityRepository: VerdictJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: VerdictData): Verdict {
        val savedJpaEntity = jpaEntityRepository.save(VerdictMapping.toJpaEntity(data))
        val verdictId = savedJpaEntity.requireId()

        val testVerdictAssociations = VerdictMapping.toTestVerdictAssociations(verdictId, data.testVerdicts)
        val savedTestVerdicts = testVerdictJpaEntityRepository.saveAll(testVerdictAssociations)

        val domainEntity = VerdictMapping.toDomain(savedJpaEntity, savedTestVerdicts.sortedBy { it.testId })
        return domainEntity
    }

    override fun update(entity: Verdict): Verdict = throw UnsupportedOperationException(
        "verdict ${entity.id.value} cannot be updated: every field of a verdict is fixed on creation",
    )

    override fun removeRoot(id: VerdictId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val jpaEntity = touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        val verdictId = jpaEntity.requireId()
        testVerdictJpaEntityRepository.deleteAll(testVerdictJpaEntityRepository.findAllByVerdictIdOrderByTestIdAsc(verdictId))
        jpaEntityRepository.delete(jpaEntity)
    }

    @Transactional(readOnly = true)
    override fun findAvailableToJudge(pagination: Pagination, filter: VerdictFilter): Page<Verdict> {
        val jpaSort = JpaSort.by(
            pagination.sort.orders.map { order ->
                JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
            },
        )
        val pageable = PageRequest.of(pagination.page, pagination.size, jpaSort)
        val page = verdictJpaEntityRepository.findAll(availableToJudge(filter), pageable)
        return Page(
            content = assembleAll(page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    override fun assembleAll(rows: List<VerdictJpaEntity>): List<Verdict> {
        val outcomesByVerdict = findAllInChunks(
            ids = rows.map { row -> row.requireId() },
            find = testVerdictJpaEntityRepository::findAllByVerdictIdInOrderByVerdictIdAscTestIdAsc,
        ).groupBy { outcome -> outcome.verdictId }

        return rows.map { row -> VerdictMapping.toDomain(row, outcomesByVerdict[row.requireId()].orEmpty()) }
    }

    /**
     * Selects current successful grading verdicts of current students or participants; the author, submission, class
     * and competition conditions are added only for the values set in [filter]. A group filter requires current author
     * membership and contest assignment to the same group.
     */
    private fun availableToJudge(filter: VerdictFilter) = Specification<VerdictJpaEntity> { verdict, query, builder ->
        val graded = query.subquery(Long::class.java)
        val submission = graded.from(SubmissionJpaEntity::class.java)
        val authorId = submission.get<Long>("authorId")
        val contestId = submission.get<Long>("gradingContestId")
        val studentAuthor = builder.existsRow(
            parent = graded,
            entity = StudentDataJpaEntity::class.java,
            selected = { student -> student.get("userId") },
            conditions = { student -> listOf(builder.equal(student.get<Long>("userId"), authorId)) },
        )
        val participantAuthor = builder.existsRow(
            parent = graded,
            entity = ParticipantDataJpaEntity::class.java,
            selected = { participant -> participant.get("userId") },
            conditions = { participant -> listOf(builder.equal(participant.get<Long>("userId"), authorId)) },
        )
        val conditions = mutableListOf(
            builder.equal(submission.get<Long>("id"), verdict.get<Long>("submissionId")),
            builder.equal(submission.get<SubmissionKindJpaEnum>("kind"), SubmissionKindJpaEnum.GRADING),
            builder.equal(submission.get<SubmissionStatusJpaEnum>("status"), SubmissionStatusJpaEnum.GRADED),
            builder.equal(submission.get<GradingResultJpaEnum>("gradingResult"), GradingResultJpaEnum.SUCCESS),
            builder.equal(submission.get<Long>("gradingVerdictId"), verdict.get<Long>("id")),
            builder.or(studentAuthor, participantAuthor),
        )
        filter.authorId?.let { author -> conditions += builder.equal(authorId, author.value) }
        filter.submissionId?.let { selected -> conditions += builder.equal(submission.get<Long>("id"), selected.value) }
        filter.classId?.let { selected ->
            conditions += builder.existsRow(
                parent = graded,
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
                parent = graded,
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
                parent = graded,
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
                parent = graded,
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

        graded.select(submission.get("id")).where(*conditions.toTypedArray())
        builder.exists(graded)
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
}
