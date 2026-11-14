package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.PollEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableCell
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.api.GradingNodeChangeResult
import tech.testsys.infra.grpc.api.GradingNodeEntry
import tech.testsys.infra.grpc.api.GradingNodeImportCandidate
import tech.testsys.infra.grpc.api.GradingNodeImportOutcome
import tech.testsys.infra.grpc.api.GradingNodeImportPreview
import tech.testsys.infra.grpc.api.GradingNodeImportReport
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingNodeRegistryOverview
import tech.testsys.infra.grpc.api.GradingNodeRejection
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.settings.api.registry.StoredGradingNode
import tech.testsys.infra.settings.api.service.SettingsPersistence
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.error.ServiceAccessDeniedView
import tech.testsys.web.service.security.ServiceSignIn

class ServiceGradingViewTests : ServiceMockVaadinTests() {
    @Autowired
    private lateinit var registry: GradingNodeRegistry

    @Autowired
    private lateinit var queue: GradingQueueMonitor

    @Autowired
    private lateinit var signIn: ServiceSignIn

    private val active = GradingNodeAddress("grader-1:50051")
    private val inactive = GradingNodeAddress("grader-2:50051")
    private val configured = GradingNodeAddress("grader-3:50051")

    @BeforeEach
    fun prepareGrading() {
        clearMocks(registry, queue)
        every { queue.snapshot() } returns GradingQueueSnapshot(preparing = 1, waiting = 2, grading = 3, saving = 4)
        every { registry.overview() } returns overview(
            GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Available(queued = 2, capacity = 4)),
            GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unreachable("UNAVAILABLE")),
        )
        every { registry.importPreview() } returns GradingNodeImportPreview(
            listOf(
                GradingNodeImportCandidate(active, StoredGradingNode(active, isActive = true)),
                GradingNodeImportCandidate(configured, existing = null),
            ),
        )
        signIn.signIn(SERVICE_TEST_SECRET)
        UI.getCurrent().navigate(ServiceGradingView::class.java)
    }

    @Test
    fun `should deny the grading page to a main cabinet principal without service authority`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            CabinetPrincipal(userId = 1, kind = UserKind.MULTIPLE_ROLE),
            null,
            listOf(SimpleGrantedAuthority("ROLE_MULTIPLE_ROLE")),
        )

        UI.getCurrent().navigate(ServiceGradingView::class.java)

        assertEquals(ServiceAccessDeniedView::class.java, currentView)
    }

    @Test
    fun `should open the grading page from the service menu`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        clickMenuLink("Проверка")

        assertEquals(ServiceGradingView::class.java, currentView)
    }

    @Nested
    inner class QueueTests {
        @Test
        fun `should show unfinished runs by state and their total in read-only fields`() {
            val counters = QUEUE_LABELS.map(::counter)

            assertEquals(listOf(1, 2, 3, 4, 10), counters.map { counter -> counter.value })
            assertTrue(counters.all { counter -> counter.isReadOnly })
        }
    }

    @Nested
    inner class NodeTests {
        @Test
        fun `should show the address the latest known availability and the participation of each node`() {
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Available(queued = 2, capacity = 4)),
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unreachable("UNAVAILABLE")),
                GradingNodeEntry(StoredGradingNode(configured, isActive = true), GradingNodeStatus.Unknown),
            )

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            assertEquals(listOf(active.target, inactive.target, configured.target), rows().map { row -> cellText(row, ADDRESS) })
            assertEquals("Доступен" to "ts-status--success", badges(active).first())
            assertEquals("Недоступен" to "ts-status--danger", badges(inactive).first())
            assertEquals("UNAVAILABLE", cellText(nodeRow(inactive), REASON))
            assertEquals("Неизвестно" to "ts-status--warning", badges(configured).first())
            assertEquals(listOf("Участвует" to "ts-status--success"), participationBadges(active))
            assertEquals(listOf("Отключён" to "ts-status--neutral"), participationBadges(inactive))
        }

        @Test
        fun `should offer the change opposite to the saved participation of each node`() {
            assertEquals(listOf("Отключить", "Удалить"), menuItems(active))
            assertEquals(listOf("Включить", "Удалить"), menuItems(inactive))
        }

        @ParameterizedTest
        @CsvSource("0, ts-status--success", "2, ts-status--warning", "4, ts-status--danger")
        fun `should colour the queue badge by the node load`(queued: Int, tone: String) {
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Available(queued = queued, capacity = 4)),
            )

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            assertEquals("В очереди: $queued/4" to tone, badges(active).last())
        }

        @Test
        fun `should show no queue badge for an unknown or unavailable node`() {
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unreachable("UNAVAILABLE")),
                GradingNodeEntry(StoredGradingNode(configured, isActive = true), GradingNodeStatus.Unknown),
            )

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            assertEquals(listOf("Недоступен" to "ts-status--danger"), badges(inactive))
            assertEquals(listOf("Неизвестно" to "ts-status--warning"), badges(configured))
        }

        @Test
        fun `should offer the configuration import in an empty registry`() {
            every { registry.overview() } returns overview()
            every { registry.importPreview() } returns
                GradingNodeImportPreview(listOf(GradingNodeImportCandidate(configured, existing = null)))

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            assertEquals("Проверяющих узлов нет", UI.getCurrent()._get<Span> { classes = "ts-empty__title" }.text)
            assertEquals(
                "Адресов в конфигурации грейдера: 1. Импортируйте их или добавьте узел по адресу.",
                UI.getCurrent()._get<Span> { classes = "ts-empty__desc" }.text,
            )
            assertTrue(UI.getCurrent()._find<Button> { text = "Импортировать из конфигурации" }.all { button -> button.isEnabled })
        }

        @Test
        fun `should suggest adding a node when the registry and configuration are empty`() {
            every { registry.overview() } returns overview()
            every { registry.importPreview() } returns GradingNodeImportPreview(emptyList())

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            assertEquals("Добавьте узел по его gRPC-адресу.", UI.getCurrent()._get<Span> { classes = "ts-empty__desc" }.text)
            assertFalse(UI.getCurrent()._get<Button> { text = "Импортировать из конфигурации" }.isEnabled)
        }

        @Test
        fun `should disable the import when every configured address is registered`() {
            every { registry.importPreview() } returns GradingNodeImportPreview(
                listOf(GradingNodeImportCandidate(active, StoredGradingNode(active, isActive = true))),
            )

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            assertFalse(UI.getCurrent()._get<Button> { text = "Импортировать из конфигурации" }.isEnabled)
        }

        @Test
        fun `should lock changes when the saved registry is uncertain`() {
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Unknown),
                persistence = SettingsPersistence.Uncertain,
            )

            UI.getCurrent().navigate(ServiceHomeView::class.java)
            UI.getCurrent().navigate(ServiceGradingView::class.java)

            val page = UI.getCurrent()._get<ServiceGradingView>().element.textRecursively
            assertTrue(page.contains("Сохранённое состояние реестра неопределённо. Изменения заблокированы до перезапуска"), page)
            assertFalse(UI.getCurrent()._get<Button> { text = "Добавить" }.isEnabled)
            assertFalse(UI.getCurrent()._get<Button> { text = "Импортировать из конфигурации" }.isEnabled)
            assertTrue(nodeMenu(active).items.none { item -> item.isEnabled })
        }
    }

    @Nested
    inner class AddTests {
        @Test
        fun `should add the entered node and close the dialog`() {
            every { registry.add("grader-4:50051", false) } returns GradingNodeChangeResult.Applied
            UI.getCurrent()._get<Button> { text = "Добавить" }._click()
            openDialog()._get<TextField>()._value = "grader-4:50051"
            openDialog()._get<Checkbox>()._value = false

            openDialog()._get<Button> { text = "Добавить" }._click()

            verify(exactly = 1) { registry.add("grader-4:50051", false) }
            assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
            assertEquals("Проверяющий узел добавлен", lastToastTitle())
        }

        @Test
        fun `should keep the dialog open with a field error when the address is refused`() {
            every { registry.add(any(), any()) } returns GradingNodeChangeResult.Rejected(GradingNodeRejection.AlreadyRegistered)
            UI.getCurrent()._get<Button> { text = "Добавить" }._click()
            openDialog()._get<TextField>()._value = active.target

            openDialog()._get<Button> { text = "Добавить" }._click()

            val address = openDialog()._get<TextField>()
            assertTrue(address.isInvalid)
            assertEquals("Узел с этим адресом уже есть в реестре", address.errorMessage)
        }

        @Test
        fun `should keep the dialog open and report a failure when the change fails`() {
            every { registry.add(any(), any()) } returns GradingNodeChangeResult.Failed
            UI.getCurrent()._get<Button> { text = "Добавить" }._click()
            openDialog()._get<TextField>()._value = "grader-4:50051"

            openDialog()._get<Button> { text = "Добавить" }._click()

            val address = openDialog()._get<TextField>()
            assertEquals("grader-4:50051", address.value)
            assertFalse(address.isInvalid)
            assertEquals("Не удалось применить изменение", lastToastTitle())
        }

        @Test
        fun `should close the dialog and lock the page when the outcome is uncertain`() {
            every { registry.add(any(), any()) } returns GradingNodeChangeResult.Uncertain
            UI.getCurrent()._get<Button> { text = "Добавить" }._click()
            openDialog()._get<TextField>()._value = "grader-4:50051"
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Unknown),
                persistence = SettingsPersistence.Uncertain,
            )

            openDialog()._get<Button> { text = "Добавить" }._click()

            assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
            assertEquals("Исход сохранения не определён", lastToastTitle())
            assertFalse(UI.getCurrent()._get<Button> { text = "Добавить" }.isEnabled)
        }

        @Test
        fun `should start a new dialog with default values`() {
            UI.getCurrent()._get<Button> { text = "Добавить" }._click()
            openDialog()._get<TextField>()._value = "grader-4:50051"
            openDialog()._get<Button> { text = "Отменить" }._click()

            UI.getCurrent()._get<Button> { text = "Добавить" }._click()

            assertEquals("", openDialog()._get<TextField>().value)
            assertTrue(openDialog()._get<Checkbox>().value)
        }

        @Test
        fun `should hint the address format in the add dialog`() {
            UI.getCurrent()._get<Button> { text = "Добавить" }._click()

            assertEquals("IP:port", openDialog()._get<TextField>().helperText)
        }
    }

    @Nested
    inner class ParticipationTests {
        @Test
        fun `should exclude a participating node from grading at once`() {
            every { registry.setActive(active, false) } returns GradingNodeChangeResult.Applied

            nodeMenu(active)._clickItemWithCaption("Отключить")

            verify(exactly = 1) { registry.setActive(active, false) }
            assertEquals("Проверяющий узел отключён от проверки", lastToastTitle())
        }

        @Test
        fun `should include an excluded node in grading at once`() {
            every { registry.setActive(inactive, true) } returns GradingNodeChangeResult.Applied

            nodeMenu(inactive)._clickItemWithCaption("Включить")

            verify(exactly = 1) { registry.setActive(inactive, true) }
            assertEquals("Проверяющий узел участвует в проверке", lastToastTitle())
        }

        @Test
        fun `should show the saved participation after a change`() {
            every { registry.setActive(active, false) } returns GradingNodeChangeResult.Applied
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = false), GradingNodeStatus.Available(queued = 2, capacity = 4)),
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unreachable("UNAVAILABLE")),
            )

            nodeMenu(active)._clickItemWithCaption("Отключить")

            assertEquals(listOf("Включить", "Удалить"), menuItems(active))
            assertEquals(listOf("Отключён" to "ts-status--neutral"), participationBadges(active))
        }

        @Test
        fun `should report a confirmed failure of a participation change`() {
            every { registry.setActive(inactive, true) } returns GradingNodeChangeResult.Failed

            nodeMenu(inactive)._clickItemWithCaption("Включить")

            assertEquals("Не удалось применить изменение", lastToastTitle())
        }

        @Test
        fun `should report an uncertain participation change and lock the page`() {
            every { registry.setActive(inactive, true) } returns GradingNodeChangeResult.Uncertain
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Unknown),
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unknown),
                persistence = SettingsPersistence.Uncertain,
            )

            nodeMenu(inactive)._clickItemWithCaption("Включить")

            assertEquals("Исход сохранения не определён", lastToastTitle())
            assertTrue(nodeMenu(inactive).items.none { item -> item.isEnabled })
            assertFalse(UI.getCurrent()._get<Button> { text = "Добавить" }.isEnabled)
        }
    }

    @Nested
    inner class RemoveTests {
        @Test
        fun `should remove a node from the row menu after confirmation`() {
            every { registry.remove(active) } returns GradingNodeChangeResult.Applied
            nodeMenu(active)._clickItemWithCaption("Удалить")

            openDialog()._get<Button> { text = "Удалить" }._click()

            verify(exactly = 1) { registry.remove(active) }
            assertEquals("Проверяющий узел удалён", lastToastTitle())
        }

        @Test
        fun `should keep a node when the removal is cancelled`() {
            nodeMenu(active)._clickItemWithCaption("Удалить")

            openDialog()._get<Button> { text = "Отменить" }._click()

            verify(exactly = 0) { registry.remove(any()) }
        }
    }

    @Nested
    inner class ImportTests {
        private val other = GradingNodeAddress("grader-4:50051")

        @BeforeEach
        fun prepareImport() {
            every { registry.importPreview() } returns GradingNodeImportPreview(
                listOf(
                    GradingNodeImportCandidate(active, StoredGradingNode(active, isActive = true)),
                    GradingNodeImportCandidate(configured, existing = null),
                    GradingNodeImportCandidate(other, existing = null),
                ),
            )
            UI.getCurrent()._get<Button> { text = "Импортировать из конфигурации" }._click()
        }

        @Test
        fun `should list new addresses as participating and registered ones without a choice`() {
            val dialog = openDialog()

            assertEquals(listOf(true, true), dialog._find<Checkbox>().map { checkbox -> checkbox.value })
            assertTrue(dialog.element.textRecursively.contains("уже в реестре · участвует"))
            assertTrue(dialog._find<Button> { text = "Выбрать все" }.isEmpty())
            assertEquals(1, dialog._find<Button> { text = "Убрать все" }.size)
        }

        @Test
        fun `should clear every choice and offer selecting all again`() {
            openDialog()._get<Button> { text = "Убрать все" }._click()

            val dialog = openDialog()
            assertEquals(listOf(false, false), dialog._find<Checkbox>().map { checkbox -> checkbox.value })
            assertEquals(1, dialog._find<Button> { text = "Выбрать все" }.size)
            assertTrue(dialog._find<Button> { text = "Убрать все" }.isEmpty())
        }

        @Test
        fun `should import new addresses with chosen participation and show each outcome`() {
            val selection = mapOf(configured to false, other to true)
            every { registry.importNodes(selection) } returns GradingNodeImportReport(
                outcomes = listOf(
                    GradingNodeImportOutcome(configured, GradingNodeChangeResult.Applied),
                    GradingNodeImportOutcome(other, GradingNodeChangeResult.Applied),
                ),
                notAttempted = emptyList(),
            )
            openDialog()._find<Checkbox>().first()._value = false

            openDialog()._get<Button> { text = "Импортировать" }._click()

            verify(exactly = 1) { registry.importNodes(selection) }
            val report = openDialog()
            assertTrue(report.element.textRecursively.contains("добавлен · отключён"))
            assertTrue(report.element.textRecursively.contains("добавлен · участвует"))
            assertEquals("Проверяющие узлы импортированы", lastToastTitle())
        }

        @Test
        fun `should show addresses left after an import stop without a success notification`() {
            every { registry.importNodes(any()) } returns GradingNodeImportReport(
                outcomes = listOf(GradingNodeImportOutcome(configured, GradingNodeChangeResult.Uncertain)),
                notAttempted = listOf(other),
            )

            openDialog()._get<Button> { text = "Импортировать" }._click()

            val report = openDialog().element.textRecursively
            assertTrue(report.contains("исход не определён; импорт остановлен"))
            assertTrue(report.contains("не добавлен: импорт остановлен"))
            assertTrue(UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.isEmpty())
        }
    }

    @Nested
    inner class RefreshTests {
        @Test
        fun `should poll every five seconds while the page is open`() {
            assertEquals(5_000, UI.getCurrent().pollInterval)
        }

        @Test
        fun `should update counters and the unavailability reason on a poll without rebuilding the page`() {
            val counter = counter("Всего незавершённых")
            val row = nodeRow(inactive)
            every { queue.snapshot() } returns GradingQueueSnapshot(preparing = 0, waiting = 0, grading = 1, saving = 0)
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Available(queued = 2, capacity = 4)),
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unreachable("DEADLINE_EXCEEDED")),
            )

            poll()

            assertSame(counter, counter("Всего незавершённых"))
            assertSame(row, nodeRow(inactive))
            assertEquals(1, counter("Всего незавершённых").value)
            assertEquals("DEADLINE_EXCEEDED", cellText(nodeRow(inactive), REASON))
        }

        @Test
        fun `should rebuild node badges on a poll when a node state or queue changes`() {
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Available(queued = 4, capacity = 4)),
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unknown),
            )

            poll()

            assertEquals(listOf("Доступен" to "ts-status--success", "В очереди: 4/4" to "ts-status--danger"), badges(active))
            assertEquals(listOf("Неизвестно" to "ts-status--warning"), badges(inactive))
            assertEquals("", cellText(nodeRow(inactive), REASON))
        }

        @Test
        fun `should rebuild the page on a poll when the registry composition changes`() {
            every { registry.overview() } returns overview(
                GradingNodeEntry(StoredGradingNode(active, isActive = true), GradingNodeStatus.Unknown),
                GradingNodeEntry(StoredGradingNode(inactive, isActive = false), GradingNodeStatus.Unknown),
                GradingNodeEntry(StoredGradingNode(configured, isActive = true), GradingNodeStatus.Unknown),
            )

            poll()

            assertEquals(listOf("Отключить", "Удалить"), menuItems(configured))
        }

        @Test
        fun `should stop polling after leaving the page`() {
            UI.getCurrent().navigate(ServiceHomeView::class.java)
            clearMocks(registry, answers = false)

            poll()

            assertEquals(-1, UI.getCurrent().pollInterval)
            verify(exactly = 0) { registry.overview() }
        }
    }

    private fun overview(vararg nodes: GradingNodeEntry, persistence: SettingsPersistence = SettingsPersistence.Known) =
        GradingNodeRegistryOverview(nodes.toList(), persistence)

    private fun poll() {
        val ui = UI.getCurrent()
        ComponentUtil.fireEvent(ui, PollEvent(ui, true))
    }

    private fun counter(label: String): IntegerField =
        UI.getCurrent()._find<IntegerField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun rows(): List<TableRow> = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>()

    private fun nodeRow(address: GradingNodeAddress): TableRow = rows().single { row -> cellText(row, ADDRESS) == address.target }

    private fun cell(row: TableRow, index: Int): TableCell = row._find<TableCell>()[index]

    private fun cellText(row: TableRow, index: Int): String = cell(row, index).element.textRecursively

    private fun menuItems(address: GradingNodeAddress): List<String> = nodeMenu(address).items.map { item -> item.element.textRecursively }

    private fun badges(address: GradingNodeAddress): List<Pair<String, String>> = badgesOf(cell(nodeRow(address), STATUS))

    private fun participationBadges(address: GradingNodeAddress): List<Pair<String, String>> =
        badgesOf(cell(nodeRow(address), PARTICIPATION))

    private fun badgesOf(cell: TableCell): List<Pair<String, String>> = cell._find<Span> { classes = "ts-status" }
        .map { badge -> badge.element.textRecursively to badge.classNames.single { name -> name.startsWith("ts-status--") } }

    private fun nodeMenu(address: GradingNodeAddress): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Действия с узлом ${address.target}"
        }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private companion object {
        const val ADDRESS = 0
        const val STATUS = 1
        const val REASON = 2
        const val PARTICIPATION = 3
        val QUEUE_LABELS = listOf(
            "Подготовка",
            "Ожидание отправки",
            "Проверка на узле",
            "Сохранение результата",
            "Всего незавершённых",
        )
    }
}
