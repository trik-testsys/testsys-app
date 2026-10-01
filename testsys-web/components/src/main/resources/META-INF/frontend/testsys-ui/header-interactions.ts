import './header-arrivals.ts';

const headers = new WeakMap<HTMLElement, () => void>();
const searches = new WeakMap<HTMLInputElement, () => void>();
const searchDelay = 300;
const interactive = 'a[href], button:not([disabled]), [tabindex="0"]';

function send(input: HTMLInputElement, name: string, detail: object) {
  input.dispatchEvent(new CustomEvent(name, {detail}));
}

(window as any).testsysHeader = {
  attach(root: HTMLElement) {
    this.detach(root);
    const keydown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k' && !event.defaultPrevented) {
        const input = root.querySelector<HTMLInputElement>('.ts-header__search input');
        if (input) { event.preventDefault(); input.focus(); }
      }
      const target = event.composedPath()[0] as HTMLElement;
      if (root.contains(target) && target.matches('.ts-header-mega-trigger > button') &&
          ['ArrowDown', 'ArrowUp'].includes(event.key)) {
        event.preventDefault();
        const popup = target.nextElementSibling as any;
        popup.opened = true;
        requestAnimationFrame(() => popup.querySelector(interactive)?.focus());
      }
      const menu = target.closest('.ts-header-user-menu') ??
        (target.matches('vaadin-popover[theme~="ts-header-user-popup"]') ? target.querySelector('.ts-header-user-menu') : null);
      if (!menu || !['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
      event.preventDefault();
      const entries = Array.from(menu.querySelectorAll<HTMLElement>('[role="menuitem"]:not([aria-disabled="true"])'));
      const current = entries.indexOf(target);
      const next = event.key === 'Home' ? 0 : event.key === 'End' ? entries.length - 1 :
        current < 0 ? (event.key === 'ArrowUp' ? entries.length - 1 : 0) :
        (current + (event.key === 'ArrowUp' ? -1 : 1) + entries.length) % entries.length;
      entries[next]?.focus();
    };
    const restoreMegaFocus = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      const trigger = Array.from(root.querySelectorAll<HTMLButtonElement>('.ts-header-mega-trigger > button'))
        .find(button => (button.nextElementSibling as any)?.opened);
      if (!trigger) return;
      const popup = trigger.nextElementSibling as any;
      requestAnimationFrame(() => { if (!popup.opened && trigger.isConnected) trigger.focus(); });
    };
    const readAllFocus = (event: MouseEvent) => {
      const button = (event.composedPath()[0] as HTMLElement).closest<HTMLButtonElement>('.ts-header-read-all');
      if (!button || !root.contains(button) || button.disabled) return;
      const popup = button.closest('vaadin-popover[theme~="ts-header-notifications-popup"]') as any;
      if (!popup?.opened || !popup.isConnected) return;
      const next = popup.querySelector('.ts-header-notifications-list button:not([disabled])') ?? popup.target;
      if (next?.isConnected) next.focus();
    };
    document.addEventListener('click', readAllFocus, true);
    document.addEventListener('keydown', restoreMegaFocus, true);
    document.addEventListener('keydown', keydown);
    const observer = new MutationObserver(() => { if (!root.isConnected) this.detach(root); });
    observer.observe(document, {childList: true, subtree: true});
    headers.set(root, () => {
      document.removeEventListener('click', readAllFocus, true);
      document.removeEventListener('keydown', restoreMegaFocus, true);
      document.removeEventListener('keydown', keydown);
      observer.disconnect();
      root.querySelectorAll<HTMLInputElement>('.ts-header__search input').forEach(input => this.searchDetach(input));
    });
  },
  detach(root: HTMLElement) { headers.get(root)?.(); headers.delete(root); },
  searchAttach(input: HTMLInputElement, popup: any) {
    this.searchDetach(input);
    let timer: ReturnType<typeof setTimeout> | undefined;
    const change = () => {
      clearTimeout(timer);
      // The eager event invalidates old answers before the delayed provider request.
      send(input, 'header-input', {query: input.value});
      if (input.value.trim()) timer = setTimeout(() => send(input, 'header-query', {query: input.value}), searchDelay);
    };
    const keydown = (event: KeyboardEvent) => {
      if (!['ArrowDown', 'ArrowUp', 'Enter', 'Escape', 'Tab'].includes(event.key)) return;
      if (event.key !== 'Tab') event.preventDefault();
      if (event.key === 'Escape' || event.key === 'Tab') clearTimeout(timer);
      send(input, 'header-key', {key: event.key});
    };
    const dismiss = () => {
      clearTimeout(timer);
      popup.opened = false;
      send(input, 'header-key', {key: 'Escape'});
    };
    const outside = (event: Event) => {
      const path = event.composedPath();
      const body = popup.querySelector('.ts-header-search-body');
      if (popup.opened && !path.includes(input) && !path.includes(popup) &&
          !path.some(node => node instanceof Node && body?.contains(node))) dismiss();
    };
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !event.defaultPrevented && event.target !== input && popup.opened) dismiss();
    };
    const pointerdown = (event: Event) => {
      // Suggestion clicks retain the combobox focus until the application opens the destination.
      if ((event.target as HTMLElement).closest('[role="option"]')) event.preventDefault();
    };
    document.addEventListener('pointerdown', outside, true);
    document.addEventListener('keydown', escape);
    input.addEventListener('input', change);
    input.addEventListener('keydown', keydown);
    popup.addEventListener('pointerdown', pointerdown);
    searches.set(input, () => {
      clearTimeout(timer);
      document.removeEventListener('pointerdown', outside, true);
      document.removeEventListener('keydown', escape);
      input.removeEventListener('input', change);
      input.removeEventListener('keydown', keydown);
      popup.removeEventListener('pointerdown', pointerdown);
    });
  },
  searchDetach(input: HTMLInputElement) { searches.get(input)?.(); searches.delete(input); }
};
