import {ArrivalTimer} from './arrival-timer.ts';

type State = { observer: MutationObserver; hide: () => void; stop: () => void; effects: Set<() => void> };
const widgets = new WeakMap<HTMLElement, State>();
const pulses = new WeakMap<HTMLElement, () => void>();

(window as any).testsysHeaderArrivals = {
  attach(widget: HTMLElement) {
    this.detach(widget);
    const observer = new MutationObserver(() => { if (!widget.isConnected) this.detach(widget); });
    observer.observe(document, {childList: true, subtree: true});
    widgets.set(widget, {observer, hide: () => {}, stop: () => {}, effects: new Set()});
  },
  show(widget: HTMLElement, card: HTMLElement, notification: any, duration: number, revision: number) {
    const state = widgets.get(widget);
    if (!state) return;
    state.stop();
    let retired = false;
    const timer = new ArrivalTimer(() => {
      if (retired || !widget.isConnected) return;
      card.dispatchEvent(new CustomEvent('header-arrival-expired', {detail: {revision}}));
      notification.opened = false;
      cleanup();
    });
    const hoverIn = () => timer.pause('hover');
    const hoverOut = () => timer.resume('hover');
    const focusIn = () => timer.pause('focus');
    const focusOut = (event: FocusEvent) => { if (!card.contains(event.relatedTarget as Node)) timer.resume('focus'); };
    const cleanup = () => {
      retired = true;
      timer.cancel();
      card.removeEventListener('pointerenter', hoverIn);
      card.removeEventListener('pointerleave', hoverOut);
      card.removeEventListener('focusin', focusIn);
      card.removeEventListener('focusout', focusOut);
      notification.removeEventListener('opened-changed', closed);
      card.classList.remove('ts-header-arrival--enter');
    };
    const closed = (event: Event) => { if (!(event as CustomEvent).detail.value) cleanup(); };
    state.stop = cleanup;
    state.hide = () => { cleanup(); notification.opened = false; };
    card.addEventListener('pointerenter', hoverIn);
    card.addEventListener('pointerleave', hoverOut);
    card.addEventListener('focusin', focusIn);
    card.addEventListener('focusout', focusOut);
    notification.addEventListener('opened-changed', closed);
    card.classList.remove('ts-header-arrival--enter');
    void card.offsetWidth;
    card.classList.add('ts-header-arrival--enter');
    const reasons = [];
    if (card.matches(':hover')) reasons.push('hover');
    if (card.matches(':focus-within')) reasons.push('focus');
    timer.start(duration, reasons);
  },
  highlight(widget: HTMLElement, row: HTMLElement) {
    const state = widgets.get(widget);
    if (!state) return;
    row.classList.add('ts-header-notification--arrival');
    const timer = window.setTimeout(cleanup, 2400);
    function cleanup() {
      window.clearTimeout(timer);
      row.classList.remove('ts-header-notification--arrival');
      state?.effects.delete(cleanup);
    }
    state.effects.add(cleanup);
  },
  pulse(widget: HTMLElement, dot: HTMLElement) {
    const state = widgets.get(widget);
    if (!state) return;
    pulses.get(dot)?.();
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    dot.classList.remove('ts-bell__dot--arrival');
    void dot.offsetWidth;
    dot.classList.add('ts-bell__dot--arrival');
    const cleanup = () => {
      dot.classList.remove('ts-bell__dot--arrival');
      dot.removeEventListener('animationend', cleanup);
      state.effects.delete(cleanup);
      pulses.delete(dot);
    };
    pulses.set(dot, cleanup);
    dot.addEventListener('animationend', cleanup, {once: true});
    state.effects.add(cleanup);
  },
  hide(widget: HTMLElement) { widgets.get(widget)?.hide(); },
  detach(widget: HTMLElement) {
    const state = widgets.get(widget);
    if (!state) return;
    state.hide();
    state.effects.forEach(cleanup => cleanup());
    state.observer.disconnect();
    widgets.delete(widget);
  }
};
