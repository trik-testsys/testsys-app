/** Whether the focus left with a closed popup: it stayed inside [popup] or fell back to [body]. */
export function shouldRestoreFocus(active: Element | null, popup: Element, body: Element): boolean {
  return active === body || popup.contains(active);
}

/** Returns focus to [trigger] only if it was lost with [popup], so a click elsewhere keeps its target. */
export function restoreFocus(trigger: HTMLElement, popup: Element): void {
  if (shouldRestoreFocus(document.activeElement, popup, document.body)) trigger.focus();
}

(window as any).testsysPopupFocus = {restore: restoreFocus};
