package tech.testsys.web.app.config

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.model.task.Submission

/**
 * [Grader] passing a submission to [delegate] by [sendToGrade] or [restartGrading] only after the current transaction
 * commits; inside a transaction [sendToGrade] answers [GradingAdmission.Accepted] at once. Otherwise it calls [delegate].
 *
 * @since 1.0.0-beta.1
 */
class AfterCommitGrader(private val delegate: Grader) : Grader by delegate {
    override fun sendToGrade(submission: Submission): GradingAdmission {
        if (!afterCommit { delegate.sendToGrade(submission) }) return delegate.sendToGrade(submission)

        return GradingAdmission.Accepted
    }

    override fun restartGrading(submission: Submission) {
        if (!afterCommit { delegate.restartGrading(submission) }) delegate.restartGrading(submission)
    }

    /** Registers [action] to run after the current transaction commits; returns `false` outside a transaction. */
    private fun afterCommit(action: () -> Unit): Boolean {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return false

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() = action()
            },
        )

        return true
    }
}
