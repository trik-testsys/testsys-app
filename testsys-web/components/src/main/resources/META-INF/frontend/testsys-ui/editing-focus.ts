interface Focusable extends HTMLElement {
  disabled?: boolean;
  readOnly?: boolean;
}

const COMPOUND_PARTS = 'vaadin-date-picker, vaadin-time-picker, .ts-lookup__text';

/** Whether [node] is shown, enabled and editable, so the user can type into it right away. */
export function isEligible(node: Focusable): boolean {
  return node.isConnected && node.getClientRects().length > 0
    && getComputedStyle(node).visibility !== 'hidden'
    && !node.closest('[hidden], [disabled], [readonly], [aria-disabled="true"]')
    && !node.disabled && !node.readOnly;
}

/**
 * Focuses the first eligible input of the editing body in the block of [fallback]: the first available part of a
 * compound field, otherwise the field itself. Without such an input [fallback] gets the focus.
 */
export function focusFirstEditableInput(fallback: Focusable): void {
  const body = fallback.closest('.ts-block')?.querySelector('[data-ts-editing-body]');
  for (const field of Array.from(body?.querySelectorAll<Focusable>('[data-ts-input]') ?? [])) {
    if (!isEligible(field)) continue;
    const parts = Array.from(field.querySelectorAll<Focusable>(COMPOUND_PARTS));
    const target = (parts.length ? parts : [field]).find(isEligible);
    if (target) { target.focus(); return; }
  }
  if (isEligible(fallback)) fallback.focus();
}

// The edit state reaches the client in the same response, so the search waits for the next frame.
(window as any).testsysEditingFocus = {
  focusFirst: (fallback: Focusable) => requestAnimationFrame(() => focusFirstEditableInput(fallback)),
};
