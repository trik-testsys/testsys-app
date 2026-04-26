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
 * Builder for constructing [DeveloperSolutionData].
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionDataBuilder : Builder<DeveloperSolutionData> {

    /**
     * The name of the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * The description of the contest.
     *
     * @since %CURRENT_VERSION%
     */
    var description: String? = null

    /**
     * The ID of the solution associated with this developer solution.
     *
     * @since %CURRENT_VERSION%
     */
    var solution: SolutionId? = null

    /**
     * The ID of the expected verdict for this developer solution.
     *
     * @since %CURRENT_VERSION%
     */
    var expectedScore: Score? = null

    var versionBucket: UUID? = null

    /**
     * Sets the [solution] from a raw ID value.
     *
     * @param solution the raw solution ID.
     * @since %CURRENT_VERSION%
     */
    fun solution(solution: Long) {
        this.solution = SolutionId(solution)
    }

    /**
     * Sets the [expectedScore] from a raw ID value.
     *
     * @param expectedScore the raw verdict ID.
     * @since %CURRENT_VERSION%
     */
    fun expectedScore(expectedScore: Int) {
        this.expectedScore = Score(expectedScore)
    }

    /**
     * Builds the [DeveloperSolutionData] instance.
     *
     * @return the constructed [DeveloperSolutionData].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [DeveloperSolution] domain entities.
 *
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionBuilder :
    DomainEntityWithDataBuilder<DeveloperSolution, DeveloperSolutionData, DeveloperSolutionDataBuilder>() {

    override fun dataBuilder() = DeveloperSolutionDataBuilder()

    /**
     * Builds the [DeveloperSolution] instance.
     *
     * @return the constructed [DeveloperSolution].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): DeveloperSolution {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return DeveloperSolution(
            id = DeveloperSolutionId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
