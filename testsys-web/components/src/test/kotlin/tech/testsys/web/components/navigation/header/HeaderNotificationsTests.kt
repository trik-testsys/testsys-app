package tech.testsys.web.components.navigation.header

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation
import com.vaadin.flow.signals.local.ValueSignal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts

class HeaderNotificationsTests : MockVaadinTests() {
    @Test
    fun `should treat the initial snapshot as a baseline without an arrival card`() {
        val source = ValueSignal(listOf(notification("old", mutableListOf())))

        val controller = notifications(source)

        assertFalse(controller.arrival.isOpen)
    }

    @Test
    fun `should show one arrival card without marking a new notification read`() {
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source)

        source.set(listOf(notification("new", mutableListOf())))

        assertTrue(controller.arrival.isOpen)
        assertEquals("new", controller.arrival.title.text)
        assertTrue(source.peek().single().isUnread)
    }

    @Test
    fun `should avoid replaying removed keys and read state or label updates`() {
        val source = ValueSignal(listOf(notification("old", mutableListOf()).copy(isUnread = false)))
        val controller = notifications(source)
        source.set(emptyList())

        source.set(listOf(notification("old", mutableListOf()).copy(label = "Changed")))

        assertFalse(controller.arrival.isOpen)
    }

    @Test
    fun `should close an arrival without changing the unread source`() {
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source)
        source.set(listOf(notification("new", mutableListOf())))

        controller.arrival.close()

        assertFalse(controller.arrival.isOpen)
        assertTrue(source.peek().single().isUnread)
    }

    @Test
    fun `should open a single arrival with the current read handler and destination`() {
        val calls = mutableListOf<String>()
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source) { key -> calls.add("read:$key") }
        source.set(listOf(notification("new", calls)))

        controller.arrival.openDestination()

        assertEquals(listOf("read:new", "open:new"), calls)
        assertFalse(controller.arrival.isOpen)
    }

    @Test
    fun `should group a batch into one card that opens the list without marking it read`() {
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source)

        source.set(listOf(notification("one", mutableListOf()), notification("two", mutableListOf())))

        assertEquals(testTexts.header.arrivalCount(2), controller.arrival.title.text)
        controller.arrival.openDestination()
        assertTrue(controller.popup.isOpened)
        assertTrue(source.peek().all { item -> item.isUnread })
    }

    @Test
    fun `should highlight an arrival in the open list without a separate card`() {
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source)
        controller.popup.open()

        source.set(listOf(notification("new", mutableListOf())))

        assertFalse(controller.arrival.isOpen)
        assertTrue(controller.popup.isOpened)
    }

    @Test
    fun `should close an arrival on detach and baseline updates during reconnect`() {
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source)
        source.set(listOf(notification("one", mutableListOf())))

        UI.getCurrent().remove(controller.component)
        source.set(listOf(notification("two", mutableListOf())))
        UI.getCurrent().add(controller.component)

        assertFalse(controller.arrival.isOpen)
    }

    @Test
    fun `should close a removed arrival without opening its stale destination`() {
        val calls = mutableListOf<String>()
        val source = ValueSignal<List<HeaderNotification>>(emptyList())
        val controller = notifications(source)
        source.set(listOf(notification("new", calls)))
        source.set(emptyList())

        controller.arrival.openDestination()

        assertTrue(calls.isEmpty())
        assertFalse(controller.arrival.isOpen)
    }

    @Test
    fun `should call read before opening an unread notification without changing its data`() {
        val calls = mutableListOf<String>()
        val notification = notification("first", calls)
        val source = ValueSignal(listOf(notification))
        val controller = notifications(source) { key -> calls.add("read:$key") }

        assertInstanceOf(NativeButton::class.java, controller.component.find("ts-header-notification"))._click()

        assertEquals(listOf("read:first", "open:first"), calls)
        assertEquals(listOf(notification), source.peek())
        assertTrue(source.peek().single().isUnread)
    }

    @Test
    fun `should preserve the row and use the latest destination when its signal changes`() {
        val calls = mutableListOf<String>()
        val source = ValueSignal(listOf(notification("first", calls)))
        val controller = notifications(source)
        val row = assertInstanceOf(NativeButton::class.java, controller.component.find("ts-header-notification"))
        source.set(
            listOf(
                HeaderNotification(
                    key = "first",
                    label = "Changed",
                    destination = HeaderDestination.Action { calls.add("new") },
                    isUnread = false,
                ),
            ),
        )

        row._click()

        assertSame(row, controller.component.find("ts-header-notification"))
        assertEquals(listOf("new"), calls)
        assertEquals("Changed", row.element.getAttribute("aria-label"))
    }

    @Test
    fun `should avoid requesting focus when only read flags change passively`() {
        val source = ValueSignal(listOf(notification("first", mutableListOf())))
        val controller = notifications(source)
        controller.popup.open()
        pendingJavaScript()

        source.set(source.peek().map { item -> item.copy(isUnread = false) })

        assertTrue(pendingJavaScript().none { call -> "focus" in call.invocation.expression })
        assertTrue(controller.popup.isOpened)
    }

    @Test
    fun `should render changed signal data without opening a closed popup`() {
        val source = ValueSignal(listOf(notification("first", mutableListOf())))
        val controller = notifications(source)

        source.set(emptyList())

        assertFalse(controller.popup.isOpened)
        assertEquals(testTexts.header.notificationsEmpty, controller.component.find("ts-header-notifications-empty").element.text)
        assertFalse(controller.indicator.isVisible)
    }

    @Test
    fun `should let the application mark all notifications read`() {
        val source = ValueSignal(listOf(notification("first", mutableListOf())))
        val controller = HeaderNotificationsController(
            HeaderNotifications(source, onRead = {}, onReadAll = { source.set(emptyList()) }),
            testTexts.header,
            HeaderInteractions(),
        )
        UI.getCurrent().add(controller.component)
        controller.popup.open()

        controller.readAll._click()

        assertTrue(source.peek().isEmpty())
        assertTrue(controller.popup.isOpened)
        assertFalse(controller.indicator.isVisible)
    }

    private fun pendingJavaScript(): List<PendingJavaScriptInvocation> {
        val internals = UI.getCurrent().internals
        internals.stateTree.runExecutionsBeforeClientResponse()
        return internals.dumpPendingJavaScriptInvocations()
    }

    private fun notifications(
        source: ValueSignal<List<HeaderNotification>>,
        onRead: (String) -> Unit = {},
    ): HeaderNotificationsController = HeaderNotificationsController(
        HeaderNotifications(source, onRead = onRead, onReadAll = {}),
        testTexts.header,
        HeaderInteractions(),
    ).also { controller -> UI.getCurrent().add(controller.component) }

    private fun notification(key: String, calls: MutableList<String>): HeaderNotification = HeaderNotification(
        key = key,
        label = key,
        isUnread = true,
        destination = HeaderDestination.Action { calls.add("open:$key") },
    )
}
