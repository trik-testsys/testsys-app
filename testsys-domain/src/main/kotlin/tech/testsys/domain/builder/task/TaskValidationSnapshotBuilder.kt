package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.*

/**
 * Builder of [TaskValidationSnapshot].
 *
 * @property tests the concrete polygon identifiers.
 * @property developerSolutions the author programs and expected scores.
 * @property supportedTrikStudioVersions the execution environment versions.
 * @since %CURRENT_VERSION%
 */
class TaskValidationSnapshotBuilder : Builder<TaskValidationSnapshot> {
    var tests: MutableList<TestId> = mutableListOf()
    var developerSolutions: MutableList<DeveloperSolutionValidationInput> = mutableListOf()
    var supportedTrikStudioVersions: MutableList<TrikStudioVersion> = mutableListOf()

    /**
     * Sets [tests] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun tests(tests: Iterable<Long>) { this.tests = tests.map { TestId(it) }.toMutableList() }

    override fun build(): TaskValidationSnapshot = TaskValidationSnapshot(
        tests = tests.distinct().sortedBy { it.value }.lazify(),
        developerSolutions = developerSolutions.toList(),
        supportedTrikStudioVersions = supportedTrikStudioVersions.distinct().sortedBy { it.version },
    )
}

/**
 * Builder of [DeveloperSolutionValidationInput]. Required: [developerSolution], [solution], [expectedScore].
 *
 * @property developerSolution the author solution version identifier, or `null` if not set.
 * @property solution the program identifier, or `null` if not set.
 * @property expectedScore the expected score, or `null` if not set.
 * @since %CURRENT_VERSION%
 */
class DeveloperSolutionValidationInputBuilder : Builder<DeveloperSolutionValidationInput> {
    var developerSolution: DeveloperSolutionId? = null
    var solution: SolutionId? = null
    var expectedScore: Score? = null

    /**
     * Sets [developerSolution] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun developerSolution(developerSolution: Long) { this.developerSolution = DeveloperSolutionId(developerSolution) }

    /**
     * Sets [solution] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun solution(solution: Long) { this.solution = SolutionId(solution) }

    override fun build(): DeveloperSolutionValidationInput = DeveloperSolutionValidationInput(
        developerSolution = requireField(developerSolution) { ::developerSolution }.lazify(),
        solution = requireField(solution) { ::solution }.lazify(),
        expectedScore = requireField(expectedScore) { ::expectedScore },
    )
}
