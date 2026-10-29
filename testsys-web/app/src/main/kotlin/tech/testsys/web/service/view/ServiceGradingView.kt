package tech.testsys.web.service.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.BeforeLeaveEvent
import com.vaadin.flow.router.BeforeLeaveObserver
import com.vaadin.flow.router.PageTitle
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
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import tech.testsys.infra.settings.api.service.SettingsPersistence
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.overlay.confirm

/**
 * Service snapshot of grading: unfinished runs by state and the checking node registry with the latest known
 * availability and queue load of its nodes. The page refreshes the snapshot every five seconds while it is open.
 *
 * @since 1.0.0-beta.1
 */
@Route("grading")
@PageTitle("Состояние проверки")
@RolesAllowed("SERVICE")
class ServiceGradingView(
    private val headers: ServiceHeaders,
    private val registry: GradingNodeRegistry,
    private val queue: GradingQueueMonitor,
) : TestSysView(), BeforeEnterObserver, BeforeLeaveObserver {
    private val counters = QueueCounters()

    // Participation changed but not applied yet; a poll may rebuild the page, and the change must survive it.
    private val pendingParticipation = mutableMapOf<GradingNodeAddress, Boolean>()
    private var reasons = emptyMap<GradingNodeAddress, ValueSignal<String>>()
    private var shown: RegistryShape? = null
    private var polling: Registration? = null

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
        keepPendingParticipation(overview)
        counters.update(queue.snapshot())
        reasons = overview.nodes
            .mapNotNull { entry -> unreachableReason(entry.status)?.let { reason -> entry.node.address to ValueSignal(reason) } }
            .toMap()
        shown = shapeOf(overview, preview)
        page(headers.header(active = ServiceHeaders.GRADING_SECTION)) {
            head("Состояние проверки")
            row {
                block(title = "Очередь", subtitle = "Незавершённые проверки этого экземпляра по этапам") {
                    QUEUE_LABELS.forEach { (label, value) -> row { field(label, labelSize = 3, size = 21) { text(value(counters)) } } }
                }
            }
            row {
                block(title = "Проверяющие узлы") {
                    actions {
                        action("Добавить", icon = IconName.Plus) {
                            isEnabled = isKnown
                            onClick { openAddDialog() }
                        }
                        action("Импортировать из конфигурации") {
                            isEnabled = isKnown && newAddresses > 0
                            onClick { openImportDialog() }
                        }
                    }
                    nodes(overview.nodes, isKnown, configured = preview.candidates.size, newAddresses = newAddresses)
                }
            }
        }
    }

    private fun keepPendingParticipation(overview: GradingNodeRegistryOverview) {
        if (overview.persistence != SettingsPersistence.Known) {
            pendingParticipation.clear()
            return
        }

        val saved = overview.nodes.associate { entry -> entry.node.address to entry.node.isActive }
        pendingParticipation.entries.removeIf { (address, isActive) -> saved[address] == null || saved[address] == isActive }
    }

    private fun BlockScope.nodes(nodes: List<GradingNodeEntry>, isKnown: Boolean, configured: Int, newAddresses: Int) {
        if (!isKnown) {
            row { text("Сохранённое состояние реестра неопределённо. Изменения заблокированы до перезапуска") }
            if (nodes.isEmpty()) row { text("Проверяющих узлов нет") }
        } else if (nodes.isEmpty()) {
            val description = if (configured > 0) {
                "Адресов в конфигурации грейдера: $configured. Импортируйте их или добавьте узел по адресу."
            } else {
                "Добавьте узел по его gRPC-адресу."
            }
            emptyState("Проверяющих узлов нет", description, icon = IconName.Settings) {
                if (newAddresses > 0) action("Импортировать из конфигурации") { onClick { openImportDialog() } }
            }
            return
        }

        nodes.forEach { entry -> nodeRow(entry, canChange = isKnown) }
    }

    private fun BlockScope.nodeRow(entry: GradingNodeEntry, canChange: Boolean) {
        val node = entry.node
        val participation = ParticipationToggle(
            isSavedActive = node.isActive,
            isActive = pendingParticipation[node.address] ?: node.isActive,
            isChangeable = canChange,
        ) { isActive ->
            if (isActive == node.isActive) {
                pendingParticipation.remove(node.address)
            } else {
                pendingParticipation[node.address] = isActive
            }
        }

        row {
            text(node.address.target, size = 8)
            horizontal(size = 9) {
                nodeBadges(entry.status).forEach { nodeBadge -> badge(nodeBadge.text, nodeBadge.tone) }
                reasons[node.address]?.let { reason -> text(reason) }
            }
            horizontal(size = 7) {
                participation.on = mainAction("Вкл") {
                    isEnabled = canChange
                    onClick { participation.toggle() }
                }
                participation.off = destructiveAction("Выкл") {
                    isEnabled = canChange
                    onClick { participation.toggle() }
                }
                participation.apply = action("Применить") { onClick { applyParticipation(node.address, participation.isActive) } }
                destructiveAction("Удалить") {
                    isEnabled = canChange
                    onClick { confirmRemoval(node.address) }
                }
            }
        }

        participation.sync()
    }

    private fun openAddDialog() {
        addNodeDialog { address, isActive ->
            when (val result = registry.add(address, isActive)) {
                GradingNodeChangeResult.Applied -> {
                    toast(FeedbackKind.Success, "Проверяющий узел добавлен")
                    show()
                    AddNodeReply.Close
                }
                is GradingNodeChangeResult.Rejected -> AddNodeReply.Keep(rejectionText(result.reason))
                GradingNodeChangeResult.Failed -> {
                    toast(FeedbackKind.Error, FAILED_TITLE, FAILED_DESCRIPTION)
                    AddNodeReply.Keep(fieldError = null)
                }
                GradingNodeChangeResult.Uncertain -> {
                    toast(FeedbackKind.Error, UNCERTAIN_TITLE, UNCERTAIN_DESCRIPTION)
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
            if (isComplete) toast(FeedbackKind.Success, "Проверяющие узлы импортированы")
            showImportReport(report, selection)
        }.open()
    }

    private fun applyParticipation(address: GradingNodeAddress, isActive: Boolean) {
        val success = if (isActive) "Проверяющий узел участвует в проверке" else "Проверяющий узел отключён от проверки"
        pendingParticipation.remove(address)
        report(registry.setActive(address, isActive), success)
    }

    private fun confirmRemoval(address: GradingNodeAddress) {
        confirm(
            title = "Удалить Проверяющий узел «${address.target}»?",
            text = "Узел исчезнет из реестра, перестанет получать Посылки и опрашиваться. " +
                "Незавершённые проверки будут отправлены повторно в пределах лимитов.",
            action = "Удалить",
            isDanger = true,
        ) { report(registry.remove(address), "Проверяющий узел удалён") }
    }

    private fun report(result: GradingNodeChangeResult, success: String) {
        when (result) {
            GradingNodeChangeResult.Applied -> toast(FeedbackKind.Success, success)
            is GradingNodeChangeResult.Rejected -> toast(FeedbackKind.Error, rejectionText(result.reason))
            GradingNodeChangeResult.Failed -> toast(FeedbackKind.Error, FAILED_TITLE, FAILED_DESCRIPTION)
            GradingNodeChangeResult.Uncertain -> toast(FeedbackKind.Error, UNCERTAIN_TITLE, UNCERTAIN_DESCRIPTION)
        }
        show()
    }

    /** Text signals of the queue counters, updated in place between page rebuilds. */
    private class QueueCounters {
        val preparing = ValueSignal("0")
        val waiting = ValueSignal("0")
        val grading = ValueSignal("0")
        val saving = ValueSignal("0")
        val total = ValueSignal("0")

        fun update(snapshot: GradingQueueSnapshot) {
            preparing.set(snapshot.preparing.toString())
            waiting.set(snapshot.waiting.toString())
            grading.set(snapshot.grading.toString())
            saving.set(snapshot.saving.toString())
            total.set(snapshot.total.toString())
        }
    }

    /**
     * Participation of a node row: «Вкл» while [isActive] and «Выкл» otherwise; «Применить» is enabled while the row
     * [isChangeable] and [isActive] differs from [isSavedActive]. [onChange] receives every switched value.
     */
    private class ParticipationToggle(
        private val isSavedActive: Boolean,
        isActive: Boolean,
        private val isChangeable: Boolean,
        private val onChange: (Boolean) -> Unit,
    ) {
        lateinit var on: ActionHandle
        lateinit var off: ActionHandle
        lateinit var apply: ActionHandle

        var isActive = isActive
            private set

        fun toggle() {
            isActive = !isActive
            onChange(isActive)
            sync()
        }

        fun sync() {
            on.isVisible = isActive
            off.isVisible = !isActive
            apply.isEnabled = isChangeable && isActive != isSavedActive
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
        const val FAILED_TITLE = "Не удалось применить изменение"
        const val FAILED_DESCRIPTION = "Прежнее состояние реестра сохранено"
        const val UNCERTAIN_TITLE = "Исход сохранения неопределён"
        const val UNCERTAIN_DESCRIPTION = "Изменения реестра заблокированы до перезапуска"

        val QUEUE_LABELS: List<Pair<String, (QueueCounters) -> ValueSignal<String>>> = listOf(
            "Подготовка" to QueueCounters::preparing,
            "Ожидание отправки" to QueueCounters::waiting,
            "Проверка на узле" to QueueCounters::grading,
            "Сохранение результата" to QueueCounters::saving,
            "Всего незавершённых" to QueueCounters::total,
        )

        fun shapeOf(overview: GradingNodeRegistryOverview, preview: GradingNodeImportPreview) = RegistryShape(
            nodes = overview.nodes.map { entry -> entry.node },
            badges = overview.nodes.map { entry -> nodeBadges(entry.status) },
            persistence = overview.persistence,
            hasNewAddresses = preview.candidates.any { candidate -> candidate.existing == null },
        )
    }
}
