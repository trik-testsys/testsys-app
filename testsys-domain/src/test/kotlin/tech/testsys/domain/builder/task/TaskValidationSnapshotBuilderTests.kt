package tech.testsys.domain.builder.task

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.model.task.*

class TaskValidationSnapshotBuilderTests {
    @Test
    fun `should normalize polygon order and order versions by Docker image tag without retaining mutable builder lists`() {
        val source = mutableListOf(TestId(3), TestId(1), TestId(3))
        val newer = TrikStudioVersion(tag = "image:a", version = "4")
        val older = TrikStudioVersion(tag = "image:b", version = "3")
        val snapshot = taskValidationSnapshot {
            tests = source
            supportedTrikStudioVersions = mutableListOf(older, newer, older)
        }
        source.clear()

        assertEquals(listOf(TestId(1), TestId(3)), snapshot.tests.ids)
        assertEquals(listOf(newer, older), snapshot.supportedTrikStudioVersions)
        assertEquals(emptyList<DeveloperSolutionValidationInput>(), snapshot.developerSolutions)
    }

    @Test
    fun `should pin author program and expected score through the nested builder`() {
        val input = developerSolutionValidationInput {
            developerSolution(7)
            solution(8)
            expectedScore = Score(42)
        }

        assertEquals(DeveloperSolutionId(7), input.developerSolution.id)
        assertEquals(SolutionId(8), input.solution.id)
        assertEquals(Score(42), input.expectedScore)
    }

    @Test
    fun `should reject author input without an expected score`() {
        assertThrows(IllegalArgumentException::class.java) {
            developerSolutionValidationInput {
                developerSolution(7)
                solution(8)
            }
        }
    }
}
