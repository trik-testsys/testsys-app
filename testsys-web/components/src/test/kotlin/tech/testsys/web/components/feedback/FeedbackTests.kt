package tech.testsys.web.components.feedback

import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.getNotifications
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.notification.Notification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.find

class FeedbackTests : MockVaadinTests() {
    @Nested
    inner class AlertTests {
        @Test
        fun `should render alert of its kind with title and text`() {
            val alert = buildTestContent {
                alert(FeedbackKind.Warning, "Тур заморожен", text = "Таблица не обновляется")
            }.find("ts-alert")

            assertTrue("ts-alert--warning" in alert.classes())
            assertEquals("Тур заморожен", alert.find("ts-alert__title").element.textRecursively)
            assertEquals("Таблица не обновляется", alert.find("ts-alert__desc").element.textRecursively)
        }

        @Test
        fun `should announce error alert to screen readers`() {
            val alert = buildTestContent { alert(FeedbackKind.Error, "Ошибка") }.find("ts-alert")

            assertTrue("ts-alert--danger" in alert.classes())
            assertEquals("alert", alert.element.getAttribute("role"))
        }

        @Test
        fun `should span alert over its size in a block row`() {
            val alert = buildTestRow { alert(FeedbackKind.Info, "Регистрация открыта", size = 12) }.child(0)

            assertTrue("ts-alert" in alert.classes())
            assertEquals("span 12", alert.element.style.get("grid-column"))
        }
    }

    @Nested
    inner class ToastTests {
        @Test
        fun `should open toast in the bottom right corner`() {
            toast(FeedbackKind.Success, "Решение отправлено", description = "Пришлём уведомление")

            val notification = getNotifications().single()
            val card = _get<Div> { classes = "ts-toast" }
            assertTrue("ts-toast--success" in card.classes())
            assertEquals("Решение отправлено", card.find("ts-toast__title").element.textRecursively)
            assertEquals(TOAST_DURATION_MS, notification.duration)
            assertEquals(Notification.Position.BOTTOM_END, notification.position)
        }
    }
}
