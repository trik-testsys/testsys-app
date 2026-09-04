package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import java.util.UUID

/**
 * Builder of [DeveloperSolutionData]. Required: [name], [description], [solution], [expectedScore], [versionBucket].
 *
 * @property name the name of the developer solution, or `null` if not set yet.
 * @property description the description of the developer solution, or `null` if not set yet.
 * @property solution the id of the reference program, or `null` if not set yet.
 * @property expectedScore the expected score, or `null` if not set yet.
 * @property versionBucket the UUID shared by all versions of the developer solution, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionDataBuilder : Builder<DeveloperSolutionData> {

    var name: String? = null

    var description: String? = null

    var solution: SolutionId? = null

    var expectedScore: Score? = null

    var versionBucket: UUID? = null

    /**
     * Sets [solution] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    /**
     * Sets [expectedScore] from a raw score.
     *
     * @since %CURRENT_VERSION%
     */
    fun expectedScore(expectedScore: Int) {
        this.expectedScore = Score(expectedScore)
    }

    override fun build(): DeveloperSolutionData {
        val name = requireField(name) { ::name }
        val description = requireField(description) { ::description }
        val solution = requireField(solution) { ::solution }
        val expectedScore = requireField(expectedScore) { ::expectedScore }
        val versionBucket = requireField(versionBucket) { ::versionBucket }

        return DeveloperSolutionData(
            name = name,
            description = description,
            solution = solution.lazify(),
            expectedScore = expectedScore,
            versionBucket = versionBucket,
        )
    }
}

/**
 * Builder of [DeveloperSolution] entities. Required: [id], [createdAt], [version], [data].
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionBuilder :
    DomainEntityWithDataBuilder<DeveloperSolution, DeveloperSolutionData, DeveloperSolutionDataBuilder>() {

    override fun dataBuilder() = DeveloperSolutionDataBuilder()

    override fun build(): DeveloperSolution {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val version = requireField(version) { ::version }
        val data = requireField(data) { ::data }

        return DeveloperSolution(
            id = DeveloperSolutionId(id),
            createdAt = createdAt,
            version = version,
            data = data,
        )
    }
}
