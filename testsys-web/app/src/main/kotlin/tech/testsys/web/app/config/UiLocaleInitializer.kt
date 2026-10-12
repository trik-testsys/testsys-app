package tech.testsys.web.app.config

import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.annotation.SpringComponent
import org.springframework.core.env.Environment
import tech.testsys.infra.localization.bundle.SupportedRegion
import tech.testsys.web.components.texts.initializeUiLocale
import java.time.ZoneId
import java.util.Locale

/** Cookie holding the name of the [SupportedRegion] the user chose. */
internal const val REGION_COOKIE = "TESTSYS_REGION"

private const val ACCEPT_LANGUAGE = "Accept-Language"

/**
 * Binds the localization of the user's region to every new UI: the region comes from the region cookie, then from
 * the browser languages, then from `testsys.web.localization.default-region`; dates start in
 * `testsys.web.localization.default-time-zone` until the browser reports its zone.
 *
 * @since %CURRENT_VERSION%
 */
@SpringComponent
class UiLocaleInitializer(environment: Environment) : VaadinServiceInitListener {
    private val defaultRegion = environment.defaultRegion()
    private val defaultTimeZone = environment.defaultTimeZone()

    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addUIInitListener { uiEvent ->
            initializeUiLocale(uiEvent.ui, resolveRegion(VaadinRequest.getCurrent(), defaultRegion), defaultTimeZone)
        }
    }
}

/** Region of the user of [request]: the region cookie, then the best supported browser language, then [default]. */
internal fun resolveRegion(request: VaadinRequest?, default: SupportedRegion): SupportedRegion {
    val chosen = request?.cookies?.firstOrNull { cookie -> cookie.name == REGION_COOKIE }?.value
    SupportedRegion.entries.firstOrNull { region -> region.name == chosen }?.let { region -> return region }

    val header = request?.getHeader(ACCEPT_LANGUAGE) ?: return default
    // The header comes from the browser; a malformed one is treated as absent.
    val ranges = try {
        Locale.LanguageRange.parse(header)
    } catch (_: IllegalArgumentException) {
        return default
    }
    val regions = SupportedRegion.entries.associateBy { region -> region.toULocale().toLanguageTag().lowercase(Locale.ROOT) }
    return Locale.filterTags(ranges, regions.keys).firstNotNullOfOrNull { tag -> regions[tag.lowercase(Locale.ROOT)] } ?: default
}

/** Region of the users whose browser languages are not supported, from `testsys.web.localization.default-region`. */
internal fun Environment.defaultRegion(): SupportedRegion =
    SupportedRegion.valueOf(getRequiredProperty("testsys.web.localization.default-region"))

/** Zone of the dates until the browser reports its own, from `testsys.web.localization.default-time-zone`. */
internal fun Environment.defaultTimeZone(): ZoneId = ZoneId.of(getRequiredProperty("testsys.web.localization.default-time-zone"))
