package tech.testsys.web.service.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.BeforeLeaveEvent
import com.vaadin.flow.router.BeforeLeaveObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.shared.Registration
import com.vaadin.flow.signals.local.ValueSignal
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.grpc.api.GradingNodeChangeResult
import tech.testsys.infra.grpc.api.GradingNodeEntry
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingNodeRegistryOverview
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.localization.PageService
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import tech.testsys.infra.settings.api.service.SettingsPersistence
import tech.testsys.web.app.view.pageOf
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.texts.currentLocalization

/**
 * Service snapshot of grading: unfinished runs by state and a table of the checking node registry with the latest
 * known availability, queue load and participation of its nodes, where the row menu applies the opposite
 * participation at once. The page refreshes the snapshot every five seconds while it is open.
 *
 * @since 1.0.0-beta.1
 */
@Route("grading")
@RolesAllowed("SERVICE")
class ServiceGradingView(
    private val headers: ServiceHeaders,
    private val registry: GradingNodeRegistry,
    private val queue: GradingQueueMonitor,
) : TestSysView(),
    BeforeEnterObserver,
    BeforeLeaveObserver,
    HasDynamicTitle {
    private val texts: PageService get() = currentLocalization().pageService

    private val counters = QueueCounters()

    private var reasons = emptyMap<GradingNodeAddress, ValueSignal<String>>()
    private var shown: RegistryShape? = null
    private var polling: Registration? = null

    override fun getPageTitle(): String = texts.gradingTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show()
        polling?.remove()
        event.ui.pollInterval = POLL_INTERVAL_MILLIS
        polling = event.ui.addPollListener { refresh() }
    }

    override fun beforeLeave(event: BeforeLeaveEvent) {
        polling?.remove()
        polling = null
        event.ui.pollInterval = NO_POLLING
    }

    private fun refresh() {
        val overview = registry.overview()
        val preview = registry.importPreview()
        if (shapeOf(overview, preview) != shown) {
            show(overview, preview)
            return
        }

        counters.update(queue.snapshot())
        overview.nodes.forEach { entry ->
            unreachableReason(entry.status)?.let { reason -> reasons[entry.node.address]?.set(reason) }
        }
    }

    private fun show(
        overview: GradingNodeRegistryOverview = registry.overview(),
        preview: GradingNodeImportPreview = registry.importPreview(),
    ) {
        val isKnown = overview.persistence == SettingsPersistence.Known
        val newAddresses = preview.candidates.count { candidate -> candidate.existing == null }
        counters.update(queue.snapshot())
        reasons = overview.nodes
            .mapNotNull { entry -> unreachableReason(entry.status)?.let { reason -> entry.node.address to ValueSignal(reason) } }
            .toMap()
        shown = shapeOf(overview, preview)
        page(headers.header(active = ServiceHeaders.MENU_SECTION)) {
            head(texts.gradingTitle())
            row {
                block(title = texts.gradingQueue(), subtitle = texts.gradingQueueSubtitle(), size = QUEUE_BLOCK_SIZE) {
                    queueRows().forEach { (label, value) ->
                        row {
                            integerInput(label, labelSize = 14, size = 10) {
                                isEditable = false
                                bindValue(value, null)
                            }
                        }
                    }
                }
                block(title = texts.gradingNodes(), subtitle = if (isKnown) null else texts.gradingUncertainRegistry()) {
                    actions {
                        action(texts.add(), icon = IconName.Plus) {
                            isEnabled = isKnown
                            onClick { openAddDialog() }
                        }
                        action(texts.gradingImport()) {
                            isEnabled = isKnown && newAddresses > 0
                            onClick { openImportDialog() }
                        }
                    }
                    nodes(isKnown, configured = preview.candidates.size, newAddresses = newAddresses)
                }
            }
        }
    }

    private fun BlockScope.nodes(isKnown: Boolean, configured: Int, newAddresses: Int) {
        table(key = { entry: GradingNodeEntry -> entry.node.address }, fetch = { request -> pageOf(registry.overview().nodes, request) }) {
            codeColumn(texts.gradingAddress(), size = 7) { entry -> entry.node.address.target }
            column(texts.gradingStatus(), size = 5) { entry ->
                nodeBadges(entry.status).forEach { nodeBadge -> badge(nodeBadge.text, nodeBadge.tone) }
            }
            column(texts.gradingReason(), size = 6) { entry -> reasons[entry.node.address]?.let { reason -> text(reason) } }
            column(texts.gradingParticipation()) { entry ->
                if (entry.node.isActive) {
                    badge(texts.gradingParticipationActive(), Tone.Success)
                } else {
                    badge(texts.gradingParticipationInactive(), Tone.Neutral)
                }
            }
            // Two grid columns: beside the queue block one column is narrower than the menu button.
            menuColumn(size = 2, ariaLabel = { entry -> texts.gradingActions(address = entry.node.address.target) }) { entry ->
                val address = entry.node.address
                // The item names the change it applies, the opposite of the saved participation.
                if (entry.node.isActive) {
                    item(texts.gradingDeactivate(), isEnabled = isKnown) {
                        report(registry.setActive(address, false), texts.gradingDeactivated())
                    }
                } else {
                    item(texts.gradingActivate(), isEnabled = isKnown) {
                        report(registry.setActive(address, true), texts.gradingActivated())
                    }
                }
                destructiveItem(texts.delete(), isEnabled = isKnown) { confirmRemoval(address) }
            }

            val description = when {
                !isKnown -> null
                configured > 0 -> texts.gradingEmptyConfigured(configured = configured)
                else -> texts.gradingEmptyHint()
            }
            empty(texts.gradingEmpty(), description, icon = IconName.Settings) {
                if (isKnown && newAddresses > 0) action(texts.gradingImport()) { onClick { openImportDialog() } }
            }
        }
    }

    private fun openAddDialog() {
        addNodeDialog { address, isActive ->
            when (val result = registry.add(address, isActive)) {
                GradingNodeChangeResult.Applied -> {
                    toast(FeedbackKind.Success, texts.gradingAdded())
                    show()
                    AddNodeReply.Close
                }
                is GradingNodeChangeResult.Rejected -> AddNodeReply.Keep(rejectionText(result.reason))
                GradingNodeChangeResult.Failed -> {
                    toast(FeedbackKind.Error, texts.gradingFailed(), texts.gradingFailedHint())
                    AddNodeReply.Keep(fieldError = null)
                }
                GradingNodeChangeResult.Uncertain -> {
                    toast(FeedbackKind.Error, texts.gradingUncertain(), texts.gradingUncertainHint())
                    show()
                    AddNodeReply.Close
                }
            }
        }.open()
    }

    private fun openImportDialog() {
        importNodesDialog(registry.importPreview()) { selection ->
            val report = registry.importNodes(selection)
            show()
            val isComplete = report.notAttempted.isEmpty() &&
                report.outcomes.all { outcome -> outcome.result == GradingNodeChangeResult.Applied }
            if (isComplete) toast(FeedbackKind.Success, texts.gradingImported())
            showImportReport(report, selection)
        }.open()
    }

    private fun confirmRemoval(address: GradingNodeAddress) {
        confirm(
            title = texts.gradingRemoveTitle(address = address.target),
            text = texts.gradingRemoveText(),
            action = texts.delete(),
            isDanger = true,
        ) { report(registry.remove(address), texts.gradingRemoved()) }
    }

    private fun queueRows(): List<Pair<String, ValueSignal<Int?>>> = listOf(
        texts.gradingQueuePreparing() to counters.preparing,
        texts.gradingQueueWaiting() to counters.waiting,
        texts.gradingQueueGrading() to counters.grading,
        texts.gradingQueueSaving() to counters.saving,
        texts.gradingQueueTotal() to counters.total,
    )

    private fun report(result: GradingNodeChangeResult, success: String) {
        when (result) {
            GradingNodeChangeResult.Applied -> toast(FeedbackKind.Success, success)
            is GradingNodeChangeResult.Rejected -> toast(FeedbackKind.Error, rejectionText(result.reason))
            GradingNodeChangeResult.Failed -> toast(FeedbackKind.Error, texts.gradingFailed(), texts.gradingFailedHint())
            GradingNodeChangeResult.Uncertain -> toast(FeedbackKind.Error, texts.gradingUncertain(), texts.gradingUncertainHint())
        }
        show()
    }

    /** Value signals of the queue counters, updated in place between page rebuilds. */
    private class QueueCounters {
        val preparing = ValueSignal<Int?>(0)
        val waiting = ValueSignal<Int?>(0)
        val grading = ValueSignal<Int?>(0)
        val saving = ValueSignal<Int?>(0)
        val total = ValueSignal<Int?>(0)

        fun update(snapshot: GradingQueueSnapshot) {
            preparing.set(snapshot.preparing)
            waiting.set(snapshot.waiting)
            grading.set(snapshot.grading)
            saving.set(snapshot.saving)
            total.set(snapshot.total)
        }
    }

    /** Registry facts that change the page structure; counters and unavailability reasons only update their signals. */
    private data class RegistryShape(
        val nodes: List<StoredGradingNode>,
        val badges: List<List<NodeBadge>>,
        val persistence: SettingsPersistence,
        val hasNewAddresses: Boolean,
    )

    private companion object {
        const val POLL_INTERVAL_MILLIS = 5_000
        const val NO_POLLING = -1
        const val QUEUE_BLOCK_SIZE = 6

        fun shapeOf(overview: GradingNodeRegistryOverview, preview: GradingNodeImportPreview) = RegistryShape(
            nodes = overview.nodes.map { entry -> entry.node },
            badges = overview.nodes.map { entry -> nodeBadges(entry.status) },
            persistence = overview.persistence,
            hasNewAddresses = preview.candidates.any { candidate -> candidate.existing == null },
        )
    }
}
