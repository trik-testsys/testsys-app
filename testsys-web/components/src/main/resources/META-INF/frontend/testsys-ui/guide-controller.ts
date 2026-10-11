import type {Config, Driver, DriveStep} from 'driver.js';

type Labels = {previousLabel: string; nextLabel: string; doneLabel: string; closeLabel: string; progressLabel: string};
type DriverFactory = (config: Config) => Driver;
type GuideIcon = 'chevron-left' | 'chevron-right' | 'check' | 'x';

function renderIcon(button: HTMLElement, name: GuideIcon): void {
  const svg = button.ownerDocument.createElementNS('http://www.w3.org/2000/svg', 'svg');
  const attributes = {
    width: '16', height: '16', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor',
    'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'aria-hidden': 'true',
  };
  Object.entries(attributes).forEach(([key, value]) => svg.setAttribute(key, value));
  const use = button.ownerDocument.createElementNS('http://www.w3.org/2000/svg', 'use');
  use.setAttribute('href', `testsys-ui/icons.svg#${name}`);
  svg.appendChild(use);
  button.replaceChildren(svg);
}

export function visibleTarget(element: HTMLElement, root: HTMLElement): boolean {
  return root.contains(element) && element.isConnected && element.getClientRects().length > 0 &&
    !element.closest('[hidden]') && getComputedStyle(element).visibility !== 'hidden';
}

export function collectTargets(root: HTMLElement, trigger: HTMLElement): HTMLElement[] {
  if (trigger.dataset.guideStart === 'block') {
    const block = trigger.closest<HTMLElement>('.ts-block[data-guide-description]');
    return block ? [block] : [];
  }
  const targets: HTMLElement[] = [];
  const introduction = root.querySelector<HTMLElement>('[data-guide-introduction][data-guide-description]');
  if (introduction) targets.push(introduction);
  root.querySelectorAll<HTMLElement>('.ts-block[data-guide-description]').forEach(block => {
    targets.push(block);
    targets.push(...Array.from(block.querySelectorAll<HTMLElement>('[data-guide-order]'))
      .sort((left, right) => Number(left.dataset.guideOrder) - Number(right.dataset.guideOrder)));
  });
  return targets.filter(target => visibleTarget(target, root));
}

// Driver accepts HTML; page descriptions are deliberately plain text.
export function plainText(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

export function installGuide(root: HTMLElement, labels: Labels, createDriver: DriverFactory): () => void {
  let active: Driver | undefined;
  let stopSession: (() => void) | undefined;
  let move: ((direction: number) => void) | undefined;
  let isPageGuide = false;

  const stop = () => stopSession?.();
  const guardShortcut = (event: KeyboardEvent) => {
    if (!active || event.key === 'Tab') return;
    if (event.key === 'Escape') {
      event.stopImmediatePropagation();
      stop();
      return;
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.stopImmediatePropagation();
      if (isPageGuide && event.type === 'keyup') move?.(event.key === 'ArrowRight' ? 1 : -1);
      return;
    }
    // Native button activation still produces a click; page-wide Vaadin shortcuts must not receive it.
    event.stopImmediatePropagation();
  };
  const start = (event: Event) => {
    const trigger = (event.target as HTMLElement).closest<HTMLElement>('[data-guide-start]');
    if (!trigger || !root.contains(trigger)) return;
    stop();
    const targets = collectTargets(root, trigger);
    if (!targets.length) return;
    const isPage = trigger.dataset.guideStart === 'page';
    isPageGuide = isPage;
    const steps: DriveStep[] = targets.map(element => ({
      element,
      popover: {
        title: plainText(element.dataset.guideTitle ?? ''),
        description: plainText(element.dataset.guideDescription ?? ''),
      },
    }));
    const originalInert = root.inert;
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let transitioning = false;
    let refreshPending = false;
    let cardFrame: number | undefined;
    let refreshFrame: number | undefined;
    let restoreCardOpacity: (() => void) | undefined;
    let animation: Animation | undefined;
    let card: HTMLElement | undefined;
    let cardMoving = false;
    let cardDuration = 360;
    let cardDestination: DOMRect | undefined;
    let previousPosition: DOMRect | undefined;
    let renderedIndex: number | undefined;
    let closed = false;
    const scrollContainers = new Set<Element>();
    const trackScrollContainers = (element: Element | undefined) => {
      while (element) {
        if (element.scrollHeight > element.clientHeight || element.scrollWidth > element.clientWidth) {
          scrollContainers.add(element);
        }
        const tree = element.getRootNode();
        element = element.parentElement ?? (tree instanceof ShadowRoot ? tree.host : undefined);
      }
    };
    const stopNativeScrolling = () => {
      window.scrollTo({left: window.scrollX, top: window.scrollY, behavior: 'instant'});
      scrollContainers.forEach(element => {
        element.scrollTo({left: element.scrollLeft, top: element.scrollTop, behavior: 'instant'});
      });
    };
    const cancelCardMotion = () => {
      if (cardFrame !== undefined) window.cancelAnimationFrame(cardFrame);
      cardFrame = undefined;
      restoreCardOpacity?.();
      restoreCardOpacity = undefined;
      animation?.cancel();
      animation = undefined;
      placementObserver.disconnect();
      card = undefined;
      cardMoving = false;
      cardDestination = undefined;
    };
    const positionFrame = (position: DOMRect) => ({
      left: `${position.left}px`, top: `${position.top}px`, right: 'auto', bottom: 'auto', opacity: 1,
    });
    const retainCardPosition = (wrapper: HTMLElement, position: DOMRect) => {
      animation = wrapper.animate([positionFrame(position), positionFrame(position)], {duration: 1, fill: 'both'});
    };
    const retargetCardMotion = () => {
      if (closed || !card || !previousPosition || !animation) return;
      const position = card.getBoundingClientRect();
      // Moving placement must not restart an ease-in or force a late correction into a single frame.
      const correcting = cardMoving;
      const remaining = correcting ? Math.max(180, cardDuration - Number(animation.currentTime ?? 0)) : 360;
      // Read Driver's placement without the visual hold, then restore it before any paint.
      const effect = animation.effect;
      animation.effect = null;
      const destination = card.getBoundingClientRect();
      animation.effect = effect;
      if (cardMoving && destination.left === cardDestination?.left && destination.top === cardDestination?.top) return;
      animation.cancel();
      if (transitioning) { retainCardPosition(card, position); return; }
      cardMoving = true;
      cardDuration = remaining;
      cardDestination = destination;
      const current = card.animate([positionFrame(position), positionFrame(destination)], {
        duration: remaining, easing: correcting ? 'linear' : 'cubic-bezier(0.4, 0, 0.2, 1)', fill: 'both',
      });
      animation = current;
      current.onfinish = () => {
        if (closed || animation !== current) return;
        current.cancel();
        animation = undefined;
        previousPosition = undefined;
        cardMoving = false;
        placementObserver.disconnect();
        refreshPending = false;
        refresh();
      };
    };
    const observer = new MutationObserver(() => refresh());
    const placementObserver = new MutationObserver(retargetCardMotion);
    const placeCard = (wrapper: HTMLElement, restoreOpacity: () => void) => {
      if (closed || card !== wrapper || restoreCardOpacity !== restoreOpacity || !previousPosition) return;
      retainCardPosition(wrapper, previousPosition);
      restoreOpacity();
      restoreCardOpacity = undefined;
      placementObserver.observe(wrapper, {attributes: true, attributeFilter: ['style']});
      if (!transitioning) retargetCardMotion();
    };
    const cleanup = () => {
      if (closed) return;
      closed = true;
      cancelCardMotion();
      if (refreshFrame !== undefined) window.cancelAnimationFrame(refreshFrame);
      refreshFrame = undefined;
      stopNativeScrolling();
      scrollContainers.clear();
      observer.disconnect();
      window.removeEventListener('scroll', refresh, true);
      motion.removeEventListener('change', motionChanged);
      active = undefined;
      stopSession = undefined;
      move = undefined;
      root.inert = originalInert;
    };
    const close = () => {
      if (closed) return;
      cleanup();
      driver.destroy();
      // Driver restores captured focus after onDestroyed; the launch button must receive the final focus.
      if (visibleTarget(trigger, root)) trigger.focus({preventScroll: true});
    };
    const drive = (index: number) => {
      const wrapper = driver.getState().popover?.wrapper;
      previousPosition = motion.matches ? undefined : wrapper?.getBoundingClientRect();
      cancelCardMotion();
      if (wrapper && previousPosition) retainCardPosition(wrapper, previousPosition);
      driver.drive(index);
      // Driver hides the old live wrapper while preparing its replacement halfway through the highlight.
      if (previousPosition && wrapper?.isConnected && driver.getState().popover?.wrapper === wrapper) {
        wrapper.style.display = 'block';
      }
    };
    const moveStep = (direction: number) => {
      if (closed || transitioning || cardMoving || cardFrame !== undefined || restoreCardOpacity) return;
      let index = (driver.getActiveIndex() ?? 0) + direction;
      while (index >= 0 && index < targets.length && !visibleTarget(targets[index], root)) index += direction;
      if (index >= 0 && index < targets.length) drive(index);
      else if (direction > 0) close();
    };
    const refresh = () => {
      if (closed) return;
      if (!root.isConnected) { close(); return; }
      if (transitioning) { refreshPending = true; return; }
      const target = targets[driver.getActiveIndex() ?? 0];
      if (target && !visibleTarget(target, root)) {
        if (cardMoving) refreshPending = true;
        else moveStep(1);
      } else driver.refresh();
    };
    const motionChanged = () => {
      if (closed) return;
      driver.setConfig({...driver.getConfig(), animate: !motion.matches, smoothScroll: !motion.matches});
      if (!motion.matches) return;
      cancelCardMotion();
      stopNativeScrolling();
      previousPosition = undefined;
      // Re-driving replaces Driver's pending callback and renders even before its halfway point.
      driver.drive(driver.getActiveIndex());
    };
    root.inert = true;
    const driver = createDriver({
      steps, animate: !motion.matches, duration: 240, smoothScroll: !motion.matches, overlayOpacity: 0.55,
      disableActiveInteraction: true, allowKeyboardControl: true, overlayClickBehavior: () => {},
      popoverClass: 'ts-guide-popover', showProgress: isPage, progressText: plainText(labels.progressLabel),
      showButtons: isPage ? ['previous', 'next', 'close'] : ['close'],
      onPopoverRender: popover => {
        if (closed) return;
        renderIcon(popover.previousButton, 'chevron-left');
        popover.previousButton.setAttribute('aria-label', labels.previousLabel);
        renderIcon(popover.nextButton, driver.hasNextStep() ? 'chevron-right' : 'check');
        popover.nextButton.setAttribute('aria-label', driver.hasNextStep() ? labels.nextLabel : labels.doneLabel);
        popover.closeButton.setAttribute('aria-label', labels.closeLabel);
        renderIcon(popover.closeButton, 'x');
        popover.wrapper.setAttribute('aria-modal', 'true');
        const index = driver.getActiveIndex();
        if (renderedIndex !== index && !motion.matches) {
          cancelCardMotion();
          const opacity = popover.wrapper.style.getPropertyValue('opacity');
          const priority = popover.wrapper.style.getPropertyPriority('opacity');
          const restoreOpacity = () => {
            if (opacity) popover.wrapper.style.setProperty('opacity', opacity, priority);
            else popover.wrapper.style.removeProperty('opacity');
          };
          restoreCardOpacity = restoreOpacity;
          popover.wrapper.style.setProperty('opacity', '0');
          card = popover.wrapper;
          if (previousPosition) {
            // Driver places and focuses the new wrapper after this hook, in the same frame.
            queueMicrotask(() => placeCard(popover.wrapper, restoreOpacity));
          } else {
            // Driver places the new wrapper after this hook; measure on the next frame.
            cardFrame = window.requestAnimationFrame(() => {
              if (closed || restoreCardOpacity !== restoreOpacity) return;
              cardFrame = undefined;
              const current = popover.wrapper.animate([
                {transform: 'translate(0px, 4px)', opacity: 0},
                {transform: 'translate(0, 0)', opacity: 1},
              ], {duration: 180, easing: getComputedStyle(popover.wrapper).getPropertyValue('--ease-out').trim()});
              animation = current;
              cardMoving = true;
              current.onfinish = () => {
                if (closed || animation !== current) return;
                animation = undefined;
                cardMoving = false;
                if (refreshPending) { refreshPending = false; refresh(); }
              };
              restoreOpacity();
              restoreCardOpacity = undefined;
            });
          }
        }
        renderedIndex = index;
        popover.closeButton.focus({preventScroll: true});
      },
      onHighlightStarted: element => {
        if (closed) return;
        transitioning = true;
        trackScrollContainers(element);
      },
      onHighlighted: () => {
        if (closed) return;
        transitioning = false;
        // The native active element is committed after the hook; move only after that commit.
        if (previousPosition) queueMicrotask(retargetCardMotion);
        if (!refreshPending) return;
        refreshPending = false;
        // Driver commits its active element after this hook, before the next frame.
        if (refreshFrame !== undefined) window.cancelAnimationFrame(refreshFrame);
        refreshFrame = window.requestAnimationFrame(() => { refreshFrame = undefined; refresh(); });
      },
      onNextClick: () => moveStep(1), onPrevClick: () => moveStep(-1), onDoneClick: close,
      onCloseClick: close, onDestroyStarted: close, onDestroyed: cleanup,
    });
    active = driver;
    stopSession = close;
    move = moveStep;
    motion.addEventListener('change', motionChanged);
    // Nested scroll events do not bubble to Driver's window listener.
    window.addEventListener('scroll', refresh, true);
    observer.observe(root, {childList: true, subtree: true, attributes: true, attributeFilter: ['hidden', 'style', 'class']});
    driver.drive();
  };
  root.addEventListener('click', start);
  window.addEventListener('vaadin-navigated', stop);
  window.addEventListener('keydown', guardShortcut, true);
  window.addEventListener('keyup', guardShortcut, true);
  return () => {
    stop();
    root.removeEventListener('click', start);
    window.removeEventListener('vaadin-navigated', stop);
    window.removeEventListener('keydown', guardShortcut, true);
    window.removeEventListener('keyup', guardShortcut, true);
  };
}
