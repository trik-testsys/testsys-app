const menuSearches = new WeakMap<HTMLInputElement, () => void>();
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
      const menuInput = Array.from(root.querySelectorAll<HTMLInputElement>('.ts-header__search input'))
        .find(input => input.getAttribute('aria-controls') === popup.id);
      requestAnimationFrame(() => { if (!popup.opened && trigger.isConnected) (menuInput ?? trigger).focus(); });
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
      root.querySelectorAll<HTMLInputElement>('.ts-header__search input').forEach(input => {
        this.searchDetach(input);
        this.menuSearchDetach(input);
      });
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
  searchDetach(input: HTMLInputElement) { searches.get(input)?.(); searches.delete(input); },
  menuSearchAttach(input: HTMLInputElement, popup: any, trigger: HTMLElement) {
    this.menuSearchDetach(input);
    let suppressFocus = false;
    const show = () => { if (!suppressFocus) send(input, 'header-menu-input', {query: input.value}); };
    const close = () => { popup.opened = false; send(input, 'header-menu-close', {}); };
    const links = () => Array.from(popup.querySelectorAll<HTMLElement>('a[href], button:not([disabled])'));
    const keydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault(); suppressFocus = true; close(); input.focus(); suppressFocus = false;
      }
      else if (event.target === input && event.key === 'Enter') {
        event.preventDefault(); links()[0]?.click();
      }
      else if (event.target === input && ['ArrowDown', 'ArrowUp'].includes(event.key)) {
        event.preventDefault(); show();
        requestAnimationFrame(() => { const entries = links(); (event.key === 'ArrowUp' ? entries.at(-1) : entries[0])?.focus(); });
      }
      else if (event.target !== input && ['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
        event.preventDefault();
        const entries = links();
        const target = (event.composedPath()[0] as HTMLElement).closest<HTMLElement>('a[href], button');
        const index = entries.indexOf(target!);
        const next = event.key === 'Home' ? 0 : event.key === 'End' ? entries.length - 1 :
          (index + (event.key === 'ArrowUp' ? -1 : 1) + entries.length) % entries.length;
        entries[next]?.focus();
      }
    };
    const outside = (event: PointerEvent) => {
      const path = event.composedPath();
      if (popup.opened && !path.includes(input) && !path.includes(popup) && !path.includes(popup.target) && !path.includes(trigger) &&
          !path.some(node => node instanceof Node && popup.contains(node))) close();
    };
    input.addEventListener('focus', show);
    input.addEventListener('input', show);
    input.addEventListener('keydown', keydown);
    popup.addEventListener('keydown', keydown);
    document.addEventListener('pointerdown', outside, true);
    menuSearches.set(input, () => {
      input.removeEventListener('focus', show);
      input.removeEventListener('input', show);
      input.removeEventListener('keydown', keydown);
      popup.removeEventListener('keydown', keydown);
      document.removeEventListener('pointerdown', outside, true);
    });
  },
  menuSearchDetach(input: HTMLInputElement) { menuSearches.get(input)?.(); menuSearches.delete(input); }
};
