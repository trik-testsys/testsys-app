@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.actions

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.Bindable
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAriaBusy

/**
 * Handle of an action button.
 *
 * @property isEnabled whether the action can be clicked.
 * @property isLoading whether the action shows a spinner and ignores clicks, keeping its look; a manual change while
 * bound, and a second binding, throw [BindingActiveException].
 * @property cooldownSeconds how many seconds the action ignores clicks after each attach, for example after each opening
 * of its dialog; `0` means no cooldown, a negative value throws [IllegalArgumentException]. The cooldown does not cover
 * [clickOnEnter], so a positive value on an action clicked on Enter throws [IllegalStateException].
 * @since 1.0.0-beta.1
 */
@TestSysDsl
class ActionHandle internal constructor(
    internal val button: Button,
    private val icon: Component?,
) : ElementHandle(button) {

    private val loading = Bindable(button.element, initial = false) { value ->
        button.icon = if (value) Span().apply { addClassName(CssClass.Spinner) } else icon
        if (value) button.element.setAriaBusy(true) else button.element.setAriaBusy(null)
    }

    var isEnabled: Boolean
        get() = button.isEnabled
        set(value) {
            button.isEnabled = value
        }

    var isLoading: Boolean
        get() = loading.value
        set(value) {
            loading.value = value
        }

    private var hasEnterShortcut = false

    var cooldownSeconds: Int = 0
        set(value) {
            require(value >= 0) { "Cooldown of an action must not be negative, got $value seconds" }
            check(value == 0 || !hasEnterShortcut) { "Action clicked on Enter cannot have a cooldown, got $value seconds" }
            field = value
        }

    init {
        button.addAttachListener { _ -> if (cooldownSeconds > 0) button.element.startCooldown(cooldownSeconds) }
    }

    /**
     * Binds [isEnabled] to [signal]: every value it produces is applied at once. A manual [isEnabled] while bound,
     * and a second binding, throw [BindingActiveException].
     *
     * @since 1.0.0-beta.1
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = button.bindEnabled(signal)

    /**
     * Binds [isLoading] to [signal]: every value it produces is shown at once. A manual [isLoading] while bound,
     * and a second binding, throw [BindingActiveException].
     *
     * @since 1.0.0-beta.1
     */
    fun bindLoading(signal: Signal<Boolean>): SignalBinding<Boolean> = loading.bind(signal)

    /**
     * Runs [listener] when the action is clicked while it is not loading.
     *
     * @since 1.0.0-beta.1
     */
    fun onClick(listener: () -> Unit) {
        button.addClickListener { _ -> if (!loading.value) listener() }
    }

    /**
     * Clicks the action when Enter is pressed anywhere on the page, so that a form submits from the keyboard. The
     * shortcut ignores [cooldownSeconds], so calling it on an action with a cooldown throws [IllegalStateException].
     *
     * @since 1.0.0-beta.1
     */
    fun clickOnEnter() {
        check(cooldownSeconds == 0) { "Action with a cooldown of $cooldownSeconds seconds cannot be clicked on Enter" }
        hasEnterShortcut = true
        button.addClickShortcut(Key.ENTER)
    }
}

/**
 * Makes the button inert on the client for [seconds], showing the seconds left and a bar draining over the whole
 * cooldown, so a hasty click cannot act and the wait does not look frozen. The server state is untouched; a repeated
 * start replaces the running countdown and restarts the bar instead of ending it early.
 */
private fun Element.startCooldown(seconds: Int): PendingJavaScriptResult = executeJs(
    """
    let left = $0;
    clearInterval(this.tsCooldown);
    this.inert = true;
    this.removeAttribute('data-ts-cooldown');
    this.style.setProperty('--ts-cooldown-duration', left + 's');
    void this.offsetWidth;
    this.setAttribute('data-ts-cooldown', left);
    this.tsCooldown = setInterval(() => {
      left -= 1;
      if (left > 0) { this.setAttribute('data-ts-cooldown', left); return; }
      clearInterval(this.tsCooldown);
      this.removeAttribute('data-ts-cooldown');
      this.style.removeProperty('--ts-cooldown-duration');
      this.inert = false;
    }, 1000);
    """.trimIndent(),
    seconds,
)
