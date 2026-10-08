package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.chooser.TaskValidationExecutionChooser
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.task.*
import tech.testsys.domain.model.user.MultipleRoleUserId

/**
 * Builder of [TaskValidationRequestData]. Required: [task], [requestedBy], [snapshot] and a choice in [execution].
 *
 * @property task the task identifier, or `null` if not set.
 * @property requestedBy the initiator identifier, or `null` if not set.
 * @property snapshot the inputs, or `null` if not set.
 * @property execution the stage chooser.
 * @since %CURRENT_VERSION%
 */
class TaskValidationRequestDataBuilder : Builder<TaskValidationRequestData> {
    var task: TaskId? = null
    var requestedBy: MultipleRoleUserId? = null
    var snapshot: TaskValidationSnapshot? = null
    val execution = TaskValidationExecutionChooser()

    /**
     * Sets [task] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun task(task: Long) {
        this.task = TaskId(task)
    }

    /**
     * Sets [requestedBy] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun requestedBy(requestedBy: Long) {
        this.requestedBy = MultipleRoleUserId(requestedBy)
    }

    override fun build(): TaskValidationRequestData = TaskValidationRequestData(
        task = requireField(task) { ::task }.lazify(),
        requestedBy = requireField(requestedBy) { ::requestedBy }.lazify(),
        snapshot = requireField(snapshot) { ::snapshot },
        execution = execution.build(),
    )
}

/**
 * Builder of [TaskValidationRequest] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class TaskValidationRequestBuilder :
    DomainEntityWithDataBuilder<TaskValidationRequest, TaskValidationRequestData, TaskValidationRequestDataBuilder>() {
    override fun dataBuilder() = TaskValidationRequestDataBuilder()

    override fun build(): TaskValidationRequest = TaskValidationRequest(
        id = TaskValidationRequestId(requireField(id) { ::id }),
        createdAt = requireField(createdAt) { ::createdAt },
        data = requireField(data) { ::data },
    ).applyVersion(version)
}
