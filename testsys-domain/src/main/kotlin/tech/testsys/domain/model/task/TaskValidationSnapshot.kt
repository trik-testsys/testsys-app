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
)
