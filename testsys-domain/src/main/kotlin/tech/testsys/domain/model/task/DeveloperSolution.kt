package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import java.time.Instant

@JvmInline
value class DeveloperSolutionId(
    override val value: Long,
) : DomainId

data class DeveloperSolutionData(
    val solution: LazyEntity<SolutionId, Solution>,
    val expectedScore: Score,
)

class DeveloperSolution(
    id: DeveloperSolutionId,
    createdAt: Instant,
    val data: DeveloperSolutionData,
) : DomainEntity<DeveloperSolutionId>(id, createdAt)