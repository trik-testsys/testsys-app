@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setEditingBody

/** Finds eligible inputs in the current DOM after the edit state has reached the client. */
internal fun focusFirstEditableInput(body: Component, fallback: Component) {
    body.element.setEditingBody(true)
    // The body may be detached or hidden; queue on the visible cancel action so fallback never waits for it.
    fallback.element.executeJs(
        """
        requestAnimationFrame(() => {
            const eligible = node => node.isConnected && node.getClientRects().length > 0
                && getComputedStyle(node).visibility !== 'hidden'
                && !node.closest('[hidden], [disabled], [readonly], [aria-disabled="true"]')
                && !node.disabled && !node.readOnly;
            const body = this.closest('.ts-block')?.querySelector('[data-ts-editing-body]');
            for (const field of body?.querySelectorAll('[data-ts-input]') ?? []) {
                if (!eligible(field)) continue;
                const parts = field.querySelectorAll('vaadin-date-picker, vaadin-time-picker, .ts-lookup__text');
                const target = Array.from(parts.length ? parts : [field]).find(eligible);
                if (target) { target.focus(); return; }
            }
            if (eligible(this)) this.focus();
        });
        """.trimIndent(),
    )
}
