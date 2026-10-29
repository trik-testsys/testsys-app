package tech.testsys.domain.contract.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId

class ContestTaskResultTests {

    @Test
    fun `should keep the author task best score and submission count`() {
        val result = ContestTaskResult(
            authorId = SingleRoleUserId(7),
            taskId = TaskId(3),
            bestScore = Score(80),
            submissionCount = 4,
        )

        assertEquals(SingleRoleUserId(7), result.authorId)
        assertEquals(TaskId(3), result.taskId)
        assertEquals(Score(80), result.bestScore)
        assertEquals(4, result.submissionCount)
    }

    @Test
    fun `should accept a single submission without a best score`() {
        val result = ContestTaskResult(
            authorId = MultipleRoleUserId(7),
            taskId = TaskId(3),
            bestScore = null,
            submissionCount = 1,
        )

        assertNull(result.bestScore)
        assertEquals(1, result.submissionCount)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `should reject a result if the submission count is not positive`(submissionCount: Int) {
        assertThrows(IllegalArgumentException::class.java) {
            ContestTaskResult(
                authorId = MultipleRoleUserId(7),
                taskId = TaskId(3),
                bestScore = null,
                submissionCount = submissionCount,
            )
        }
    }
}
