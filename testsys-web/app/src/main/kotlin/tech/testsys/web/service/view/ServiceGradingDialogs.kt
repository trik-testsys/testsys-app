package tech.testsys.web.service.view

import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.api.GradingNodeChangeResult
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeImportReport
import tech.testsys.infra.grpc.api.GradingNodeRejection
import tech.testsys.infra.localization.PageService
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.text
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.checkbox
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.overlay.DialogHandle
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization

/** Texts of the service cabinet in the language of the current `UI`. */
private val texts: PageService get() = currentLocalization().pageService

/** Reply of the add dialog to a submitted address: close it, or keep it open with an optional field error. */
internal sealed interface AddNodeReply {
    /** The dialog closes. */
    data object Close : AddNodeReply

    /** The dialog stays open; [fieldError] marks the address field. */
    data class Keep(val fieldError: String?) : AddNodeReply
}

/** Builds a new add dialog with default values; [submit] receives the entered address and participation. */
internal fun addNodeDialog(submit: (address: String, isActive: Boolean) -> AddNodeReply): DialogHandle = dialog(
    title = texts.gradingAddTitle(),
    subtitle = texts.gradingAddSubtitle(),
) {
    lateinit var address: ValueInput<String>
    lateinit var participation: ValueInput<Boolean>
    row { address = textInput(texts.gradingAddAddress(), labelSize = 8, size = 16, hint = texts.gradingAddAddressHint()) }
    row { participation = checkbox(texts.gradingParticipates(), labelSize = 8, size = 16) { value = true } }
    footer { dialog ->
        action(texts.cancel()) { onClick { dialog.close() } }
        mainAction(texts.add()) {
            onClick {
                when (val reply = submit(address.value, participation.value)) {
                    AddNodeReply.Close -> dialog.close()
                    is AddNodeReply.Keep -> {
                        address.isInvalid = reply.fieldError != null
                        address.errorMessage = reply.fieldError
                    }
                }
            }
        }
    }
}

/** Builds the import dialog of [preview]; [submit] receives participation chosen for every new address. */
internal fun importNodesDialog(preview: GradingNodeImportPreview, submit: (Map<GradingNodeAddress, Boolean>) -> Unit): DialogHandle {
    val choices = linkedMapOf<GradingNodeAddress, ValueInput<Boolean>>()
    val toggle = SelectionToggle(choices.values)
    return dialog(
        title = texts.gradingImportTitle(),
        subtitle = texts.gradingImportSubtitle(),
        size = DialogSize.L,
    ) {
        preview.candidates.forEach { candidate ->
            row {
                text(candidate.address.target, size = 10)
                val existing = candidate.existing
                if (existing == null) {
                    text(texts.gradingImportNew(), size = 6)
                    choices[candidate.address] = checkbox(texts.gradingParticipates(), labelSize = 6, size = 2) {
                        value = true
                        addValueChangeListener { toggle.sync() }
                    }
                } else {
                    text(if (existing.isActive) texts.gradingImportExistingActive() else texts.gradingImportExistingInactive(), size = 14)
                }
            }
        }
        footer { dialog ->
            toggle.selectAll = action(texts.gradingImportSelectAll()) {
                isVisible = false
                onClick { choices.values.forEach { choice -> choice.value = true } }
            }
            toggle.clearAll = action(texts.gradingImportClearAll()) {
                onClick { choices.values.forEach { choice -> choice.value = false } }
            }
            action(texts.cancel()) { onClick { dialog.close() } }
            mainAction(texts.gradingImportSubmit()) {
                onClick {
                    val selection = choices.mapValues { (_, choice) -> choice.value }
                    dialog.close()
                    submit(selection)
                }
            }
        }
    }
}

/** Opens the outcome of every attempted and skipped address of an import. */
internal fun showImportReport(report: GradingNodeImportReport, selection: Map<GradingNodeAddress, Boolean>) {
    dialog(title = texts.gradingImportReport(), size = DialogSize.L) {
        report.outcomes.forEach { outcome ->
            row {
                text(outcome.address.target, size = 10)
                text(importOutcomeText(outcome.result, selection[outcome.address] == true), size = 14)
            }
        }
        report.notAttempted.forEach { address ->
            row {
                text(address.target, size = 10)
                text(texts.gradingImportStopped(), size = 14)
            }
        }
        footer { dialog -> mainAction(texts.close()) { onClick { dialog.close() } } }
    }.open()
}

/** Badge of a node row with its [text] and [tone]. */
internal data class NodeBadge(val text: String, val tone: Tone)

/** Badges of the latest known availability of a node and, for an available node, of its queue load. */
internal fun nodeBadges(status: GradingNodeStatus): List<NodeBadge> = when (status) {
    GradingNodeStatus.Unknown -> listOf(NodeBadge(texts.gradingStatusUnknown(), Tone.Warning))
    is GradingNodeStatus.Unreachable -> listOf(NodeBadge(texts.gradingStatusUnreachable(), Tone.Danger))
    is GradingNodeStatus.Available -> listOf(
        NodeBadge(texts.gradingStatusAvailable(), Tone.Success),
        NodeBadge(texts.gradingStatusQueue(queued = status.queued, capacity = status.capacity), queueTone(status.queued, status.capacity)),
    )
}

/** gRPC status code shown next to the badge of an unreachable node, or `null` for any other [status]. */
internal fun unreachableReason(status: GradingNodeStatus): String? = (status as? GradingNodeStatus.Unreachable)?.reason

private fun queueTone(queued: Int, capacity: Int): Tone = when {
    queued >= capacity -> Tone.Danger
    queued == 0 -> Tone.Success
    else -> Tone.Warning
}

/** Explains why a registry change was refused. */
internal fun rejectionText(reason: GradingNodeRejection): String = when (reason) {
    GradingNodeRejection.Blank -> texts.gradingRejectionBlank()
    GradingNodeRejection.TooLong -> texts.gradingRejectionTooLong()
    GradingNodeRejection.AlreadyRegistered -> texts.gradingRejectionAlreadyRegistered()
    GradingNodeRejection.NotRegistered -> texts.gradingRejectionNotRegistered()
}

private fun importOutcomeText(result: GradingNodeChangeResult, isActive: Boolean): String = when (result) {
    GradingNodeChangeResult.Applied -> if (isActive) texts.gradingImportAddedActive() else texts.gradingImportAddedInactive()
    is GradingNodeChangeResult.Rejected -> when (result.reason) {
        GradingNodeRejection.AlreadyRegistered -> texts.gradingImportAlreadyRegistered()
        GradingNodeRejection.NotRegistered -> texts.gradingImportNotConfigured()
        GradingNodeRejection.Blank, GradingNodeRejection.TooLong -> texts.gradingImportInvalidAddress()
    }
    GradingNodeChangeResult.Failed -> texts.gradingImportFailed()
    GradingNodeChangeResult.Uncertain -> texts.gradingImportUncertain()
}

/** Shows «Выбрать все» while some new address is unchecked and «Убрать все» otherwise. */
private class SelectionToggle(private val choices: Collection<ValueInput<Boolean>>) {
    lateinit var selectAll: ActionHandle
    lateinit var clearAll: ActionHandle

    fun sync() {
        val isAllSelected = choices.all { choice -> choice.value }
        selectAll.isVisible = !isAllSelected
        clearAll.isVisible = isAllSelected
    }
}
