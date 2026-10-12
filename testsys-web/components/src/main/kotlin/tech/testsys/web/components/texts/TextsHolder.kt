package tech.testsys.web.components.texts

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import tech.testsys.infra.localization.Localization
import java.time.ZoneId

/** Makes [localization] the localization of the pages and dialogs built on [ui]. */
internal fun bindUiLocalization(ui: UI, localization: UiLocalization) {
    ComponentUtil.setData(ui, UiLocalization::class.java, localization)
}

/** Localization bound to the current UI when it was created. */
internal fun currentUiLocalization(): UiLocalization {
    val ui = checkNotNull(UI.getCurrent()) { "No current UI: components are built in a Vaadin request or UI.access" }
    return checkNotNull(ComponentUtil.getData(ui, UiLocalization::class.java)) {
        "No localization bound to the current UI: call initializeUiLocale when the UI is created"
    }
}

/**
 * Messages of the region of the current UI; pages use them for their own texts.
 *
 * @throws IllegalStateException if there is no current UI or [initializeUiLocale] has not been called for it.
 * @since %CURRENT_VERSION%
 */
fun currentLocalization(): Localization = currentUiLocalization().messages

/**
 * Time zone of the user of the current UI, in which pages show and read dates.
 *
 * @throws IllegalStateException if there is no current UI or [initializeUiLocale] has not been called for it.
 * @since %CURRENT_VERSION%
 */
fun currentTimeZone(): ZoneId = currentUiLocalization().timeZone
