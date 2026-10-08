package tech.testsys.web.app.config

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.model.task.Submission

/**
 * [Grader] passing a submission to [delegate] only after the current transaction commits, so the grader never sees
 * a submission that is not saved; inside a transaction [sendToGrade] answers [GradingAdmission.Accepted] without
 * waiting for the delegate. Outside a transaction and for other methods it calls [delegate] at once.
 *
 * @since %CURRENT_VERSION%
 */
class AfterCommitGrader(private val delegate: Grader) : Grader by delegate {
    override fun sendToGrade(submission: Submission): GradingAdmission {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return delegate.sendToGrade(submission)

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    delegate.sendToGrade(submission)
                }
            },
        )

        return GradingAdmission.Accepted
    }
}
