import type {Config, Driver, DriveStep, PopoverDOM} from 'driver.js';

type Labels = {previousLabel: string; nextLabel: string; doneLabel: string; closeLabel: string; progressLabel: string};
type DriverFactory = (config: Config) => Driver;

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
  let observer: MutationObserver | undefined;
  let originalInert = root.inert;
  let launch: HTMLElement | undefined;
  let targets: HTMLElement[] = [];
  let isPageGuide = false;

  const stop = () => active?.destroy();
  const guardShortcut = (event: KeyboardEvent) => {
    if (!active || event.key === 'Tab' || event.key === 'Escape') return;
    if (isPageGuide && (event.key === 'ArrowLeft' || event.key === 'ArrowRight')) return;
    // Native button activation still produces a click; page-wide Vaadin shortcuts must not receive it.
    event.stopImmediatePropagation();
  };
  const move = (direction: number) => {
    if (!active) return;
    let index = (active.getActiveIndex() ?? 0) + direction;
    while (index >= 0 && index < targets.length && !visibleTarget(targets[index], root)) index += direction;
    if (index >= 0 && index < targets.length) active.drive(index);
    else if (direction > 0) stop();
  };
  const onPopoverRender = (popover: PopoverDOM) => {
    popover.previousButton.textContent = '←';
    popover.previousButton.setAttribute('aria-label', labels.previousLabel);
    popover.nextButton.textContent = active?.hasNextStep() ? '→' : '✓';
    popover.nextButton.setAttribute('aria-label', active?.hasNextStep() ? labels.nextLabel : labels.doneLabel);
    popover.closeButton.setAttribute('aria-label', labels.closeLabel);
    popover.wrapper.setAttribute('aria-modal', 'true');
    popover.closeButton.focus({preventScroll: true});
  };
  const start = (event: Event) => {
    const trigger = (event.target as HTMLElement).closest<HTMLElement>('[data-guide-start]');
    if (!trigger || !root.contains(trigger)) return;
    stop();
    targets = collectTargets(root, trigger).filter(target => visibleTarget(target, root));
    if (!targets.length) return;
    launch = trigger;
    const isPage = trigger.dataset.guideStart === 'page';
    isPageGuide = isPage;
    const steps: DriveStep[] = targets.map(element => ({
      element,
      popover: {
        title: plainText(element.dataset.guideTitle ?? ''),
        description: plainText(element.dataset.guideDescription ?? ''),
      },
    }));
    originalInert = root.inert;
    root.inert = true;
    active = createDriver({
      steps, animate: false, smoothScroll: false, overlayOpacity: 0.55,
      disableActiveInteraction: true, allowKeyboardControl: true, overlayClickBehavior: () => {},
      popoverClass: 'ts-guide-popover', showProgress: isPage, progressText: plainText(labels.progressLabel),
      showButtons: isPage ? ['previous', 'next', 'close'] : ['close'],
      onPopoverRender,
      onNextClick: () => move(1), onPrevClick: () => move(-1), onDoneClick: stop,
      onDestroyed: () => {
        observer?.disconnect();
        observer = undefined;
        active = undefined;
        root.inert = originalInert;
        if (launch?.isConnected && visibleTarget(launch, root)) launch.focus({preventScroll: true});
      },
    });
    active.drive();
    observer = new MutationObserver(() => {
      const target = targets[active?.getActiveIndex() ?? 0];
      if (!root.isConnected) stop();
      else if (target && !visibleTarget(target, root)) move(1);
      else active?.refresh();
    });
    observer.observe(root, {childList: true, subtree: true, attributes: true, attributeFilter: ['hidden', 'style', 'class']});
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
