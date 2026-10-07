package tech.testsys.domain.model.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.developerSolutionValidationInput
import tech.testsys.domain.builder.api.taskValidationSnapshot

class TaskValidationSnapshotTests {
    @Test
    fun `should order author runs by author solution identifier then by version string`() {
        val snapshot = taskValidationSnapshot {
            developerSolutions = mutableListOf(input(authorId = 20, solutionId = 7), input(authorId = 10, solutionId = 7))
            supportedTrikStudioVersions = mutableListOf(TrikStudioVersion("v2"), TrikStudioVersion("v1"))
        }

        val runs = TaskValidationSnapshot.authorRuns(snapshot)

        assertEquals(
            listOf(10L to "v1", 10L to "v2", 20L to "v1", 20L to "v2"),
            runs.map { run -> run.input.developerSolution.id.value to run.trikStudioVersion.version },
        )
    }

    @Test
    fun `should list no author runs if the snapshot has no versions`() {
        val snapshot = taskValidationSnapshot {
            developerSolutions = mutableListOf(input(authorId = 10, solutionId = 7))
        }

        assertEquals(emptyList<AuthorSolutionRun>(), TaskValidationSnapshot.authorRuns(snapshot))
    }

    private fun input(authorId: Long, solutionId: Long): DeveloperSolutionValidationInput = developerSolutionValidationInput {
        developerSolution(authorId)
        solution(solutionId)
        expectedScore = Score(1)
    }
}
