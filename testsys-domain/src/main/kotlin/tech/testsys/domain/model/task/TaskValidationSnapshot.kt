package tech.testsys.domain.model.task

import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList

/**
 * Author solution inputs of a [TaskValidationRequest].
 *
 * @property developerSolution the concrete author solution version.
 * @property solution the immutable program.
 * @property expectedScore the expected score.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionValidationInput(
    val developerSolution: LazyEntity<DeveloperSolutionId, DeveloperSolution>,
    val solution: LazyEntity<SolutionId, Solution>,
    val expectedScore: Score,
)

/**
 * The direct validation inputs pinned by a [TaskValidationRequest].
 *
 * @property tests the concrete polygon versions.
 * @property developerSolutions the author programs and expectations.
 * @property supportedTrikStudioVersions the execution environment versions.
 * @since %CURRENT_VERSION%
 */
data class TaskValidationSnapshot(
    val tests: LazyEntityList<TestId, Test>,
    val developerSolutions: List<DeveloperSolutionValidationInput>,
    val supportedTrikStudioVersions: List<TrikStudioVersion>,
) {
    /**
     * Operations on [TaskValidationSnapshot] values.
     *
     * @since %CURRENT_VERSION%
     */
    companion object {
        /**
         * Lists every author solution and TRIK Studio version pair of [snapshot] in the canonical order
         * of author submissions: by author solution identifier, then by version string.
         *
         * @since %CURRENT_VERSION%
         */
        fun authorRuns(snapshot: TaskValidationSnapshot): List<AuthorSolutionRun> {
            val versions = snapshot.supportedTrikStudioVersions.sortedBy { it.version }
            return snapshot.developerSolutions.sortedBy { it.developerSolution.id.value }.flatMap { input ->
                versions.map { version -> AuthorSolutionRun(input = input, trikStudioVersion = version) }
            }
        }
    }
}

/**
 * One author submission of a [TaskValidationSnapshot]: an author solution checked in one TRIK Studio version.
 *
 * @property input the author solution inputs.
 * @property trikStudioVersion the execution environment version.
 * @since %CURRENT_VERSION%
 */
data class AuthorSolutionRun(
    val input: DeveloperSolutionValidationInput,
    val trikStudioVersion: TrikStudioVersion,
)
