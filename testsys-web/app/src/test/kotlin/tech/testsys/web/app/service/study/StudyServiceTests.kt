package tech.testsys.web.app.service.study

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.user.StudyOperations
import tech.testsys.web.app.config.AfterCommitGrader
import tech.testsys.web.app.error.operationFailure
import tech.testsys.web.app.service.CurrentUser
import java.time.Instant

class StudyServiceTests {
    private val operations = mockk<StudyOperations>()
    private val user = mockk<SingleRoleUser>()
    private val currentUser = mockk<CurrentUser> { every { singleRoleUser() } returns user }
    private val grader = mockk<AfterCommitGrader>()
    private val service = StudyService(operations, currentUser, grader)
    private val file = FileData(uploadedFilename = "solution.py", content = byteArrayOf(1))
    private val submission = submission {
        id = 1
        createdAt = Instant.EPOCH
        data = submissionData {
            author(1)
            solution(1)
            task(TASK_ID.value)
            status.queued()
            kind.grading { contest(CONTEST_ID.value) }
        }
    }

    @Nested
    inner class SendSolutionTests {
        @Test
        fun `should pass the saved submission to grading`() {
            every { operations.sendSolution(user, CONTEST_ID, TASK_ID, file, TrikSupportedLanguage.Python) } returns
                OperationResult.Success(submission)
            every { grader.sendToGrade(submission) } returns GradingAdmission.Accepted

            val result = service.sendSolution(CONTEST_ID, TASK_ID, file, TrikSupportedLanguage.Python)

            assertEquals(submission.id, result.id)
            verify(exactly = 1) { grader.sendToGrade(submission) }
        }

        @Test
        fun `should not grade anything when the operation fails`() {
            val error = ContestNotExistsError(CONTEST_ID)
            every { operations.sendSolution(user, CONTEST_ID, TASK_ID, file, TrikSupportedLanguage.Python) } returns
                OperationResult.Error(error, operationFailure(error))

            assertThrows(OperationException::class.java) {
                service.sendSolution(CONTEST_ID, TASK_ID, file, TrikSupportedLanguage.Python)
            }

            verify(exactly = 0) { grader.sendToGrade(any()) }
        }
    }

    private companion object {
        val CONTEST_ID = ContestId(1)
        val TASK_ID = TaskId(1)
    }
}
