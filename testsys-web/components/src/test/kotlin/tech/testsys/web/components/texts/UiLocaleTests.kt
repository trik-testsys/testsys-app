package tech.testsys.web.components.texts

import com.github.mvysny.kaributesting.v10.createExtendedClientDetails
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.internal.JacksonUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.web.components.FirstTestView
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.pendingJavaScript
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.Locale

class UiLocaleTests : MockVaadinTests() {
    private val moscow = ZoneId.of("Europe/Moscow")
    private val yekaterinburg = ZoneId.of("Asia/Yekaterinburg")

    @Test
    fun `should set the locale of the UI from the region`() {
        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)

        assertEquals(Locale.forLanguageTag("ru-RU"), UI.getCurrent().locale)
    }

    @Test
    fun `should set the document language to the language tag of the region`() {
        pendingJavaScript()

        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)

        assertTrue(
            pendingJavaScript().any { call ->
                "document.documentElement.lang = \$0" in call.invocation.expression &&
                    call.invocation.parameters.any { parameter -> "ru-RU" in parameter.toString() }
            },
        )
    }

    @Test
    fun `should use the fallback zone until the browser reports its zone`() {
        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)

        assertEquals(moscow, currentTimeZone())
    }

    @Test
    fun `should start the UI in the browser zone known when it is created`() {
        UI.getCurrent().internals.extendedClientDetails = createExtendedClientDetails(tzId = "Asia/Yekaterinburg")

        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)

        assertEquals(yekaterinburg, currentTimeZone())
    }

    @Test
    fun `should apply the browser zone reported after the UI was created`() {
        val ui = UI.getCurrent()
        ui.navigate(FirstTestView::class.java)
        initializeUiLocale(ui, SupportedRegion.RU, moscow)

        pendingJavaScript().single { call -> "getBrowserDetailsParameters" in call.invocation.expression }
            .complete(JacksonUtils.createObjectNode().put("v-tzid", "Asia/Yekaterinburg"))

        assertEquals(yekaterinburg, ComponentUtil.getData(ui, UiLocalization::class.java)?.timeZone)
    }

    @Test
    fun `should rebind the UI to a different browser zone`() {
        val ui = UI.getCurrent()
        ui.navigate(FirstTestView::class.java)
        initializeUiLocale(ui, SupportedRegion.RU, moscow)

        applyBrowserZone(ui, SupportedRegion.RU, "Asia/Yekaterinburg")

        assertEquals(yekaterinburg, ComponentUtil.getData(ui, UiLocalization::class.java)?.timeZone)
    }

    @Test
    fun `should reload the page when the browser zone differs`() {
        val ui = UI.getCurrent()
        ui.navigate(FirstTestView::class.java)
        initializeUiLocale(ui, SupportedRegion.RU, moscow)

        applyBrowserZone(ui, SupportedRegion.RU, "Asia/Yekaterinburg")

        // The reload recreates the UI, as the browser would.
        assertNotSame(ui, UI.getCurrent())
    }

    @Test
    fun `should start the next UI of the session in the browser zone reported earlier`() {
        UI.getCurrent().navigate(FirstTestView::class.java)
        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)
        applyBrowserZone(UI.getCurrent(), SupportedRegion.RU, "Asia/Yekaterinburg")

        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)

        assertEquals(yekaterinburg, currentTimeZone())
    }

    @Test
    fun `should keep the page when the browser reports the zone of the UI`() {
        val ui = UI.getCurrent()
        ui.navigate(FirstTestView::class.java)
        initializeUiLocale(ui, SupportedRegion.RU, moscow)

        applyBrowserZone(ui, SupportedRegion.RU, "Europe/Moscow")

        assertSame(ui, UI.getCurrent())
    }

    @Test
    fun `should keep the zone when the browser reports an unknown zone`() {
        initializeUiLocale(UI.getCurrent(), SupportedRegion.RU, moscow)

        applyBrowserZone(UI.getCurrent(), SupportedRegion.RU, "Mars/Olympus")

        assertEquals(moscow, currentTimeZone())
    }

    @Test
    fun `should take calendar names, first weekday and date patterns from the locale data`() {
        val localization = UiLocalization(SupportedRegion.RU, moscow)

        assertEquals("Январь", localization.monthNames.first())
        assertEquals("воскресенье", localization.weekdays.first())
        assertEquals(listOf("вс", "пн"), localization.weekdaysShort.take(2))
        assertEquals(DayOfWeek.MONDAY, localization.firstDayOfWeek)
        assertEquals("dd.MM.yyyy", localization.dateFormat)
        assertEquals("HH:mm", localization.timeFormat)
    }
}
