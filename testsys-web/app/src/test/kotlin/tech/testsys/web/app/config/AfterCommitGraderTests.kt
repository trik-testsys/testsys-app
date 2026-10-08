package tech.testsys.web.app.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.domain.builder.api.submission
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.GradingAdmission
import java.time.Instant

class AfterCommitGraderTests {
    private val delegate = mockk<Grader>()
    private val grader = AfterCommitGrader(delegate)
    private val submission = submission {
        id = 1
        createdAt = Instant.EPOCH
        data = submissionData {
            author(1)
            solution(1)
            task(1)
            status.queued()
            kind.grading { contest(1) }
        }
    }

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization()
    }

    @Test
    fun `should return the answer of the delegate outside a transaction`() {
        every { delegate.sendToGrade(submission) } returns GradingAdmission.AlreadyPending

        val admission = grader.sendToGrade(submission)

        assertEquals(GradingAdmission.AlreadyPending, admission)
    }

    @Test
    fun `should not send the submission before the transaction commits`() {
        TransactionSynchronizationManager.initSynchronization()

        val admission = grader.sendToGrade(submission)

        assertEquals(GradingAdmission.Accepted, admission)
        verify(exactly = 0) { delegate.sendToGrade(any()) }
    }

    @Test
    fun `should send the submission after the transaction commits`() {
        every { delegate.sendToGrade(submission) } returns GradingAdmission.Accepted
        TransactionSynchronizationManager.initSynchronization()
        grader.sendToGrade(submission)

        TransactionSynchronizationManager.getSynchronizations().forEach { synchronization -> synchronization.afterCommit() }

        verify(exactly = 1) { delegate.sendToGrade(submission) }
    }

    @Test
    fun `should not send the submission when the transaction rolls back`() {
        TransactionSynchronizationManager.initSynchronization()
        grader.sendToGrade(submission)

        TransactionSynchronizationManager.getSynchronizations().forEach { synchronization ->
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        }

        verify(exactly = 0) { delegate.sendToGrade(any()) }
    }
}
