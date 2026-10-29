package tech.testsys.web.components.texts

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.page.Page
import com.vaadin.flow.server.VaadinRequest
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.DateTimeException
import java.time.ZoneId

/** Session attribute holding the browser time zone, so the UIs created after it is known start in it. */
private const val BROWSER_TIME_ZONE = "tech.testsys.web.components.browserTimeZone"

/** Width of the placeholder client details that Vaadin returns before the browser has sent them. */
private const val UNKNOWN_SCREEN_WIDTH = -1

/**
 * Binds the localization of [region] to a new [ui], sets the UI locale and the browser document language. Dates use
 * the browser time zone of the session, or [fallbackTimeZone] until the browser reports it; a zone reported after the
 * UI was created rebinds the localization and reloads the page once.
 *
 * @since 1.0.0-beta.1
 */
fun initializeUiLocale(ui: UI, region: SupportedRegion, fallbackTimeZone: ZoneId) {
    val details = ui.page.extendedClientDetails
    val isKnown = details.screenWidth != UNKNOWN_SCREEN_WIDTH
    if (isKnown) rememberBrowserZone(details.timeZoneId)

    val localization = UiLocalization(region, browserZone() ?: fallbackTimeZone)
    bindUiLocalization(ui, localization)
    ui.locale = localization.locale
    ui.page.setDocumentLanguage(localization.locale.toLanguageTag())

    if (!isKnown) details.refresh { received -> applyBrowserZone(ui, region, received.timeZoneId) }
}

/** Remembers the browser zone [zoneId] reported after [ui] was built; a different zone rebinds and reloads the page. */
internal fun applyBrowserZone(ui: UI, region: SupportedRegion, zoneId: String?) {
    rememberBrowserZone(zoneId)
    val zone = browserZone() ?: return
    if (zone == ComponentUtil.getData(ui, UiLocalization::class.java)?.timeZone) return

    bindUiLocalization(ui, UiLocalization(region, zone))
    ui.page.reload()
}

// The module compiles without the servlet API, so the HTTP session is reached through the current Vaadin request.
private fun rememberBrowserZone(zoneId: String?) {
    val zone = zoneId?.let(::parseZone) ?: return
    VaadinRequest.getCurrent()?.wrappedSession?.setAttribute(BROWSER_TIME_ZONE, zone)
}

private fun browserZone(): ZoneId? = VaadinRequest.getCurrent()?.wrappedSession?.getAttribute(BROWSER_TIME_ZONE) as? ZoneId

// The zone comes from the browser; an identifier the JVM does not know keeps the current zone.
private fun parseZone(zoneId: String): ZoneId? = try {
    ZoneId.of(zoneId)
} catch (_: DateTimeException) {
    null
}

private fun Page.setDocumentLanguage(languageTag: String) = executeJs("document.documentElement.lang = \$0", languageTag)
