package tech.testsys.infra.grpc.api

import org.springframework.stereotype.Component
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.infra.grpc.internal.GradingCoordinator
import tech.testsys.infra.grpc.internal.InternalGrpcApi

/**
 * Asynchronous gRPC grading adapter implementing testsys.dev.grading.balancing.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalGrpcApi::class)
@Component
class BalancingGrader internal constructor(private val coordinator: GradingCoordinator) : Grader {
    override fun sendToGrade(submission: Submission): GradingAdmission = coordinator.submit(submission)
    override fun subscribeOnGraded(onGraded: (SubmissionId) -> Unit) = coordinator.subscribe(onGraded)
    override fun addNode(address: GradingNodeAddress) = coordinator.addNode(address)
    override fun removeNode(address: GradingNodeAddress) = coordinator.removeNode(address)
    override fun getNodeStatuses(): Map<GradingNodeAddress, GradingNodeStatus> = coordinator.statuses()
}
