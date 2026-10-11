import assert from 'node:assert/strict';
import test from 'node:test';
import {collectTargets, installGuide, plainText} from '../../main/resources/META-INF/frontend/testsys-ui/guide-controller.ts';

function fixture({pending = false, reduced = false, lifecycle = false} = {}) {
  globalThis.window = new EventTarget();
  globalThis.getComputedStyle = element => ({
    visibility: element.visibility ?? 'visible', getPropertyValue: name => name === '--ease-out' ? 'cubic-bezier(0, 0, 0.2, 1)' : '',
  });
  globalThis.ShadowRoot = class { constructor(host) { this.host = host; } };
  function scrollSurface(left = 0, top = 0) {
    return {
      scrollLeft: left, scrollTop: top, scrollX: left, scrollY: top, scrolling: false, stops: [],
      clientWidth: 200, scrollWidth: 400, clientHeight: 200, scrollHeight: 1000,
      getRootNode() { return document; },
      scrollTo(options) {
        this.stops.push(options);
        this.scrolling = false;
        this.scrollLeft = this.scrollX = options.left;
        this.scrollTop = this.scrollY = options.top;
      },
      advanceScroll() {
        if (this.scrolling) { this.scrollTop += 10; this.scrollY += 10; }
      },
    };
  }
  Object.assign(window, scrollSurface(12, 80));
  const motion = new EventTarget();
  motion.matches = reduced;
  window.matchMedia = () => motion;
  const frames = new Map();
  let frameId = 0;
  window.requestAnimationFrame = callback => { frames.set(++frameId, callback); return frameId; };
  window.cancelAnimationFrame = id => frames.delete(id);
  const flushFrame = () => {
    const callbacks = [...frames.values()];
    frames.clear();
    callbacks.forEach(callback => callback());
  };
  const observers = [];
  globalThis.MutationObserver = class {
    constructor(callback) { this.callback = callback; observers.push(this); }
    observe() { this.connected = true; }
    disconnect() { this.connected = false; }
  };
  const target = (title, order) => ({
    dataset: {guideTitle: title, guideDescription: '<b>plain</b>', guideOrder: order},
    isConnected: true, hidden: false,
    rect: {left: 380, right: 580, top: 40, bottom: 100},
    getBoundingClientRect() { return this.rect; },
    getClientRects() { return this.hidden ? [] : [{}]; },
    closest() { return this.hidden ? this : null; },
    focus() { this.focused = true; },
    getRootNode() { return document; },
  });
  const intro = target('Introduction');
  const detail = target('Detail', '0');
  const block = target('Block');
  const outer = scrollSurface(5, 30);
  const inner = scrollSurface(7, 40);
  inner.parentElement = outer;
  intro.parentElement = outer;
  block.parentElement = inner;
  detail.getRootNode = () => new ShadowRoot(inner);
  block.querySelectorAll = () => [detail];
  const trigger = target('Launch');
  trigger.dataset.guideStart = 'page';
  trigger.closest = selector => selector === '[hidden]' ? null : block;
  const root = new EventTarget();
  root.inert = false;
  root.isConnected = true;
  root.contains = element => [intro, block, detail, trigger].includes(element);
  root.querySelector = () => intro;
  root.querySelectorAll = () => [block];
  root.closest = () => trigger;
  const instances = [];
  const document = {
    createElementNS(namespace, tag) { return domElement(tag, namespace); },
    querySelector() { return overlay; },
  };
  root.ownerDocument = document;
  function domElement(tag, namespace) {
    const classes = new Set();
    return {
      tag, namespace, ownerDocument: document, attributes: {}, children: [], offsetWidth: 320, isConnected: true,
      style: {
        opacity: '', priority: '', display: 'block',
        getPropertyValue(name) { return this[name] ?? ''; },
        getPropertyPriority(name) { return name === 'opacity' ? this.priority : ''; },
        setProperty(name, value, priority = '') { this[name] = value; if (name === 'opacity') this.priority = priority; },
        removeProperty(name) { this[name] = ''; if (name === 'opacity') this.priority = ''; },
      },
      rect: {left: 20, top: 40}, animations: [],
      getBoundingClientRect() {
        const current = this.animations.findLast(animation => !animation.canceled && animation.effect !== null);
        const frame = current?.keyframes[0];
        if (!frame?.left) return this.rect;
        const end = current.keyframes[1];
        const progress = Math.min(1, current.currentTime / current.options.duration);
        return {
          left: parseFloat(frame.left) + (parseFloat(end.left) - parseFloat(frame.left)) * progress,
          top: parseFloat(frame.top) + (parseFloat(end.top) - parseFloat(frame.top)) * progress,
        };
      },
      animate(keyframes, options) {
        const animation = {keyframes, options, currentTime: 0, effect: {}, canceled: false,
          cancel() { this.canceled = true; }, finish() { this.currentTime = options.duration; this.onfinish?.(); }};
        this.animations.push(animation);
        return animation;
      },
      classList: {
        additions: 0,
        add(value) { classes.add(value); this.additions++; },
        remove(value) { classes.delete(value); },
        contains(value) { return classes.has(value); },
      },
      setAttribute(key, value) { this.attributes[key] = value; },
      appendChild(child) { this.children.push(child); },
      replaceChildren(...children) { this.children = children; },
      focus() { this.focused = true; },
    };
  }
  const overlay = domElement('svg');
  let popover = {
    previousButton: domElement('button'), nextButton: domElement('button'),
    closeButton: domElement('button'), wrapper: domElement('div'),
  };
  function factory(config) {
    let index = 0;
    let committed;
    const instance = {
      config, destroyed: false, refreshes: 0, arrowSide: 'bottom',
      refresh() {
        this.refreshes++;
        window.requestAnimationFrame(() => {
          const target = committed.getBoundingClientRect();
          const wrapper = popover.wrapper.getBoundingClientRect();
          const right = wrapper.left + popover.wrapper.offsetWidth;
          this.arrowSide = target.right > wrapper.left && target.left < right ? 'bottom' :
            target.right <= wrapper.left ? 'right' : 'left';
        });
      },
      drives: [],
      drive(next = 0) {
        index = next;
        this.drives.push(next);
        config.onHighlightStarted(config.steps[index].element);
        if (lifecycle) popover.wrapper.style.display = 'none';
        if (!pending || !config.animate) this.finish();
      },
      renderHalf(position = {left: 220, top: 140}) {
        window.requestAnimationFrame(() => {
          const old = popover;
          old.wrapper.isConnected = false;
          popover = {
            previousButton: domElement('button'), nextButton: domElement('button'),
            closeButton: domElement('button'), wrapper: domElement('div'),
          };
          config.onPopoverRender(popover);
          popover.wrapper.rect = position;
        });
      },
      finish() { config.onHighlighted(); committed = config.steps[index].element; },
      getState() { return {popover, __activeElement: committed}; },
      getConfig() { return this.config; },
      setConfig(next) { Object.assign(config, next); },
      getActiveIndex() { return index; },
      hasNextStep() { return index < config.steps.length - 1; },
      destroy() { this.destroyed = true; config.onDestroyed(); },
    };
    instances.push(instance);
    return instance;
  }
  const labels = {previousLabel: 'Назад', nextLabel: 'Далее', doneLabel: 'Готово', closeLabel: 'Закрыть', progressLabel: '{{current}} из {{total}}'};
  const finishCard = () => popover.wrapper.animations.findLast(animation => !animation.canceled)?.finish();
  return {root, intro, block, detail, trigger, labels, factory, instances, observers, get popover() { return popover; }, overlay, motion,
    finishCard,
    frames, flushFrame, outer, inner};
}

test('should stop window and nested native scrolling at current coordinates after the target is visible on Escape', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onNextClick();
  f.instances[0].config.onPopoverRender(f.popover);
  f.popover.closeButton.isConnected = false;
  f.instances[0].config.onNextClick();
  window.scrolling = f.outer.scrolling = f.inner.scrolling = true;
  assert.equal(f.detail.getClientRects().length, 1);

  window.dispatchEvent(Object.assign(new Event('keydown'), {key: 'Escape'}));
  window.advanceScroll();
  f.outer.advanceScroll();
  f.inner.advanceScroll();

  assert.deepEqual(window.stops, [{left: 12, top: 80, behavior: 'instant'}]);
  assert.deepEqual(f.outer.stops, [{left: 5, top: 30, behavior: 'instant'}]);
  assert.deepEqual(f.inner.stops, [{left: 7, top: 40, behavior: 'instant'}]);
  assert.equal(window.scrollY, 80);
  assert.equal(f.outer.scrollTop, 30);
  assert.equal(f.inner.scrollTop, 40);
  assert.equal(f.trigger.focused, true);
  cleanup();
});

test('should stop native scrolling after the target becomes visible when reduced motion is enabled', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onNextClick();
  window.scrolling = f.outer.scrolling = f.inner.scrolling = true;
  assert.equal(f.block.getClientRects().length, 1);
  f.motion.matches = true;

  f.motion.dispatchEvent(new Event('change'));
  window.advanceScroll();
  f.outer.advanceScroll();
  f.inner.advanceScroll();

  assert.equal(window.scrollY, 80);
  assert.equal(f.outer.scrollTop, 30);
  assert.equal(f.inner.scrollTop, 40);
  assert.equal(f.instances[0].config.smoothScroll, false);
  cleanup();
});

test('should preserve native smooth scrolling during normal card rendering and step changes', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  window.scrolling = f.outer.scrolling = f.inner.scrolling = true;

  f.instances[0].config.onPopoverRender(f.popover);
  f.flushFrame();
  f.instances[0].config.onNextClick();
  f.instances[0].config.onPopoverRender(f.popover);
  f.flushFrame();

  assert.deepEqual(window.stops, []);
  assert.deepEqual(f.outer.stops, []);
  assert.deepEqual(f.inner.stops, []);
  assert.equal(window.scrolling && f.outer.scrolling && f.inner.scrolling, true);
  cleanup();
});

test('should hide a placed card until its next-frame animation and restore its original inline opacity', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.popover.wrapper.style.setProperty('opacity', '0.8', 'important');
  f.root.dispatchEvent(new Event('click'));
  window.requestAnimationFrame(() => {
    f.instances[0].config.onPopoverRender(f.popover);
    f.popover.wrapper.rect = {left: 220, top: 140};
  });

  f.flushFrame();

  assert.equal(f.popover.wrapper.style.opacity, '0');
  assert.equal(f.popover.wrapper.animations.length, 0);
  f.flushFrame();
  assert.equal(f.popover.wrapper.animations.length, 1);
  assert.equal(f.popover.wrapper.style.opacity, '0.8');
  assert.equal(f.popover.wrapper.style.priority, 'important');
  cleanup();
});

test('should fade the card in with a 4px vertical shift for 180ms using --ease-out when the guide opens', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);

  f.flushFrame();

  assert.deepEqual(f.popover.wrapper.animations[0].keyframes, [
    {transform: 'translate(0px, 4px)', opacity: 0},
    {transform: 'translate(0, 0)', opacity: 1},
  ]);
  assert.deepEqual(f.popover.wrapper.animations[0].options, {duration: 180, easing: 'cubic-bezier(0, 0, 0.2, 1)'});
  cleanup();
});

test('should keep card animation and deferred refresh in separate frames during a highlight', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.observers[0].callback();
  window.requestAnimationFrame(() => {
    f.instances[0].config.onPopoverRender(f.popover);
    f.instances[0].finish();
  });

  f.flushFrame();

  assert.equal(f.popover.wrapper.style.opacity, '0');
  assert.equal(f.frames.size, 2);
  f.flushFrame();
  assert.equal(f.instances[0].refreshes, 1);
  assert.equal(f.popover.wrapper.animations.length, 1);
  assert.equal(f.popover.wrapper.style.opacity, '');
  cleanup();
});

test('should restore card visibility when reduced motion is enabled before its first animation frame', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);
  assert.equal(f.popover.wrapper.style.opacity, '0');
  f.motion.matches = true;

  f.motion.dispatchEvent(new Event('change'));
  f.flushFrame();

  assert.equal(f.popover.wrapper.style.opacity, '');
  assert.equal(f.popover.wrapper.animations.length, 0);
  cleanup();
});

test('should ignore a canceled card callback after a new guide has prepared its own card', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);
  const stale = [...f.frames.values()][0];
  f.root.dispatchEvent(new Event('click'));
  assert.equal(f.popover.wrapper.style.opacity, '');
  f.instances[1].config.onPopoverRender(f.popover);

  stale();

  assert.equal(f.popover.wrapper.style.opacity, '0');
  assert.equal(f.popover.wrapper.animations.length, 0);
  f.flushFrame();
  assert.equal(f.popover.wrapper.style.opacity, '');
  assert.equal(f.popover.wrapper.animations.length, 1);
  cleanup();
});

test('should render decorative Lucide controls while preserving accessible labels and focus', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);
  assert.equal(f.popover.previousButton.children[0].children[0].attributes.href, 'testsys-ui/icons.svg#chevron-left');
  assert.equal(f.popover.nextButton.children[0].children[0].attributes.href, 'testsys-ui/icons.svg#chevron-right');
  assert.equal(f.popover.closeButton.children[0].children[0].attributes.href, 'testsys-ui/icons.svg#x');
  assert.deepEqual(f.popover.closeButton.children[0].attributes, {
    width: '16', height: '16', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor',
    'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'aria-hidden': 'true',
  });
  assert.equal(f.popover.previousButton.attributes['aria-label'], 'Назад');
  assert.equal(f.popover.nextButton.attributes['aria-label'], 'Далее');
  assert.equal(f.popover.closeButton.attributes['aria-label'], 'Закрыть');
  assert.equal(f.popover.wrapper.attributes['aria-modal'], 'true');
  assert.equal(f.popover.closeButton.focused, true);
  cleanup();
});

test('should replace next with a single check icon and done label on the last step', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);
  f.instances[0].drive(2);
  f.instances[0].config.onPopoverRender(f.popover);
  f.instances[0].config.onPopoverRender(f.popover);
  assert.equal(f.popover.nextButton.children.length, 1);
  assert.equal(f.popover.nextButton.children[0].children[0].attributes.href, 'testsys-ui/icons.svg#check');
  assert.equal(f.popover.nextButton.attributes['aria-label'], 'Готово');
  cleanup();
});

test('should transfer an opaque live card before paint and move only after the native highlight commit', async () => {
  const f = fixture({pending: true, lifecycle: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  const driver = f.instances[0];
  driver.config.onPopoverRender(f.popover);
  f.flushFrame();
  f.finishCard();
  driver.finish();
  const old = f.popover.wrapper;

  driver.config.onNextClick();

  assert.equal(old.style.display, 'block');
  assert.deepEqual(old.getBoundingClientRect(), {left: 20, top: 40});
  driver.renderHalf();
  f.flushFrame();
  assert.equal(old.isConnected, false);
  assert.equal(f.popover.wrapper.style.opacity, '0');
  await Promise.resolve();
  assert.equal(f.popover.wrapper.style.opacity, '');
  assert.deepEqual(f.popover.wrapper.getBoundingClientRect(), {left: 20, top: 40});
  assert.deepEqual(f.popover.wrapper.animations[0].keyframes[0], {
    left: '20px', top: '40px', right: 'auto', bottom: 'auto', opacity: 1,
  });
  assert.equal(driver.getState().__activeElement, f.intro);
  driver.finish();
  assert.equal(f.popover.wrapper.animations.length, 1);
  await Promise.resolve();
  assert.equal(driver.getState().__activeElement, f.block);
  const travel = f.popover.wrapper.animations[1];
  assert.deepEqual(travel.keyframes, [
    {left: '20px', top: '40px', right: 'auto', bottom: 'auto', opacity: 1},
    {left: '220px', top: '140px', right: 'auto', bottom: 'auto', opacity: 1},
  ]);
  assert.deepEqual(travel.options, {duration: 360, easing: 'cubic-bezier(0.4, 0, 0.2, 1)', fill: 'both'});
  cleanup();
});

async function movingCard() {
  const f = fixture({pending: true, lifecycle: true});
  f.cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);
  f.flushFrame();
  f.finishCard();
  f.instances[0].finish();
  f.instances[0].config.onNextClick();
  f.instances[0].renderHalf();
  f.flushFrame();
  await Promise.resolve();
  f.instances[0].finish();
  await Promise.resolve();
  return f;
}

test('should point at the current target after a geometry refresh during the first card transfer', async () => {
  const f = await movingCard();
  window.dispatchEvent(new Event('scroll'));
  f.flushFrame();
  assert.equal(f.instances[0].arrowSide, 'left');

  f.finishCard();
  f.flushFrame();

  assert.equal(f.instances[0].arrowSide, 'bottom');
  f.cleanup();
});

test('should point at the final target after correcting the card trajectory', async () => {
  const f = await movingCard();
  const wrapper = f.popover.wrapper;
  wrapper.animations[1].currentTime = 120;
  wrapper.rect = {left: 400, top: 240};
  f.block.rect = {left: 500, right: 700, top: 140, bottom: 200};
  f.observers[1].callback();
  window.dispatchEvent(new Event('scroll'));
  f.flushFrame();
  assert.equal(f.instances[0].arrowSide, 'left');

  f.finishCard();
  f.flushFrame();

  assert.equal(f.instances[0].arrowSide, 'bottom');
  f.cleanup();
});

test('should transfer back from the last visible card coordinates on a previous command', async () => {
  const f = await movingCard();
  f.finishCard();
  const old = f.popover.wrapper;

  f.instances[0].config.onPrevClick();
  f.instances[0].renderHalf({left: 20, top: 40});
  f.flushFrame();
  await Promise.resolve();

  assert.equal(old.isConnected, false);
  assert.deepEqual(f.popover.wrapper.getBoundingClientRect(), {left: 220, top: 140});
  f.instances[0].finish();
  await Promise.resolve();
  assert.deepEqual(f.popover.wrapper.animations[1].keyframes, [
    {left: '220px', top: '140px', right: 'auto', bottom: 'auto', opacity: 1},
    {left: '20px', top: '40px', right: 'auto', bottom: 'auto', opacity: 1},
  ]);
  f.cleanup();
});

test('should ignore repeated commands until both highlight and card finish', async () => {
  const f = await movingCard();

  f.instances[0].config.onNextClick();
  f.instances[0].config.onPrevClick();
  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowRight'}));

  assert.deepEqual(f.instances[0].drives, [0, 1]);
  f.finishCard();
  f.instances[0].config.onNextClick();
  assert.deepEqual(f.instances[0].drives, [0, 1, 2]);
  f.cleanup();
});

test('should continue a corrected trajectory without restarting its ease-in or rushing at the deadline', async () => {
  const f = await movingCard();
  const wrapper = f.popover.wrapper;
  const travel = wrapper.animations[1];
  travel.currentTime = 120;
  const visible = wrapper.getBoundingClientRect();
  wrapper.rect = {left: 300, top: 240};

  f.observers[1].callback();

  const retargeted = wrapper.animations[2];
  assert.equal(travel.canceled, true);
  assert.deepEqual(wrapper.getBoundingClientRect(), visible);
  assert.deepEqual(retargeted.keyframes[1], {left: '300px', top: '240px', right: 'auto', bottom: 'auto', opacity: 1});
  assert.equal(retargeted.options.duration, 240);
  assert.equal(retargeted.options.easing, 'linear');
  retargeted.currentTime = 80;
  wrapper.rect = {left: 400, top: 340};
  f.observers[1].callback();
  assert.equal(wrapper.animations[3].options.duration, 180);
  f.cleanup();
});

test('should spread a late far placement correction across frames instead of jumping at the old deadline', async () => {
  const f = await movingCard();
  const wrapper = f.popover.wrapper;
  wrapper.animations[1].currentTime = 350;
  const visible = wrapper.getBoundingClientRect();
  wrapper.rect = {left: 1220, top: 640};

  f.observers[1].callback();
  wrapper.animations[2].currentTime = 6;

  const continued = wrapper.getBoundingClientRect();
  assert.ok(continued.left > visible.left);
  assert.ok(continued.left - visible.left < 40);
  assert.ok(continued.top - visible.top < 20);
  f.cleanup();
});

test('should refresh committed geometry on scroll during card travel', async () => {
  const f = await movingCard();

  window.dispatchEvent(new Event('scroll'));

  assert.equal(f.instances[0].refreshes, 1);
  assert.equal(f.instances[0].getState().__activeElement, f.block);
  f.cleanup();
});

test('should release the scroll listener when the guide closes during card travel', async () => {
  const f = await movingCard();
  f.cleanup();

  window.dispatchEvent(new Event('scroll'));

  assert.equal(f.instances[0].refreshes, 0);
});

test('should preserve the current easing when Driver rewrites an unchanged placement', async () => {
  const f = await movingCard();
  const wrapper = f.popover.wrapper;
  const travel = wrapper.animations[1];
  travel.currentTime = 120;
  const visible = wrapper.getBoundingClientRect();

  f.observers[1].callback();

  assert.equal(wrapper.animations.length, 2);
  assert.equal(travel.canceled, false);
  assert.equal(travel.currentTime, 120);
  assert.deepEqual(wrapper.getBoundingClientRect(), visible);
  f.cleanup();
});

test('should cancel placement and ignore its stale callbacks immediately on Escape', async () => {
  const f = await movingCard();
  const travel = f.popover.wrapper.animations[1];
  const completed = travel.onfinish;

  window.dispatchEvent(Object.assign(new Event('keydown'), {key: 'Escape'}));
  f.observers[1].callback();
  completed();

  assert.equal(travel.canceled, true);
  assert.equal(f.observers[1].connected, false);
  assert.equal(f.popover.wrapper.animations.length, 2);
  assert.equal(f.root.inert, false);
  assert.equal(f.trigger.focused, true);
  assert.equal(f.instances[0].refreshes, 0);
  f.cleanup();
});

test('should ignore a queued handoff after navigation closes the guide', async () => {
  const f = fixture({pending: true, lifecycle: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].finish();
  f.instances[0].config.onNextClick();
  f.instances[0].renderHalf();
  f.flushFrame();

  window.dispatchEvent(new Event('vaadin-navigated'));
  await Promise.resolve();

  assert.equal(f.popover.wrapper.style.opacity, '');
  assert.equal(f.popover.wrapper.animations.length, 0);
  assert.equal(f.root.inert, false);
  cleanup();
});

test('should cancel continuous card travel and native scrolling when reduced motion is enabled', async () => {
  const f = await movingCard();
  const travel = f.popover.wrapper.animations[1];
  window.scrolling = f.outer.scrolling = f.inner.scrolling = true;
  f.motion.matches = true;

  f.motion.dispatchEvent(new Event('change'));
  await Promise.resolve();

  assert.equal(travel.canceled, true);
  assert.equal(f.observers[1].connected, false);
  assert.equal(f.instances[0].config.animate, false);
  assert.equal(window.scrolling || f.outer.scrolling || f.inner.scrolling, false);
  assert.deepEqual(f.instances[0].drives, [0, 1, 1]);
  f.cleanup();
});

test('collects page introduction, block and detailed steps in order', () => {
  const f = fixture();
  assert.deepEqual(collectTargets(f.root, f.trigger), [f.intro, f.block, f.detail]);
});

test('a block launch contains that block and its detailed steps', () => {
  const f = fixture();
  f.trigger.dataset.guideStart = 'block';
  assert.deepEqual(collectTargets(f.root, f.trigger), [f.block, f.detail]);
});

test('should show progress and navigation buttons when a block guide has detailed steps', () => {
  const f = fixture();
  f.trigger.dataset.guideStart = 'block';
  const cleanup = installGuide(f.root, f.labels, f.factory);

  f.root.dispatchEvent(new Event('click'));

  const config = f.instances[0].config;
  assert.deepEqual(config.showButtons, ['previous', 'next', 'close']);
  assert.equal(config.showProgress, true);
  cleanup();
});

test('should move a block guide to its detailed step on ArrowRight', () => {
  const f = fixture();
  f.trigger.dataset.guideStart = 'block';
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));

  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowRight'}));

  assert.equal(f.instances[0].getActiveIndex(), 1);
  cleanup();
});

test('should return a block guide to its explanation on ArrowLeft', () => {
  const f = fixture();
  f.trigger.dataset.guideStart = 'block';
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].drive(1);

  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowLeft'}));

  assert.equal(f.instances[0].getActiveIndex(), 0);
  cleanup();
});

test('skips hidden and detached page targets', () => {
  const f = fixture();
  f.detail.hidden = true;
  f.block.isConnected = false;
  assert.deepEqual(collectTargets(f.root, f.trigger), [f.intro]);
});

test('escapes markup and entities for the library HTML API', () => {
  assert.equal(plainText('<img onerror="run()"> &'), '&lt;img onerror=&quot;run()&quot;&gt; &amp;');
});

test('launches Driver with read-only page and escaped text', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  assert.equal(f.root.inert, true);
  assert.equal(f.instances[0].config.disableActiveInteraction, true);
  assert.equal(f.instances[0].config.steps[0].popover.description, '&lt;b&gt;plain&lt;/b&gt;');
  assert.equal(f.instances[0].config.animate, true);
  assert.equal(f.instances[0].config.duration, 240);
  assert.equal(f.instances[0].config.smoothScroll, true);
  cleanup();
});

test('next skips targets that become hidden after launch', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.block.hidden = true;
  f.instances[0].config.onNextClick();
  assert.equal(f.instances[0].getActiveIndex(), 2);
  cleanup();
});

test('previous keeps the first step open', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPrevClick();
  assert.equal(f.instances[0].getActiveIndex(), 0);
  assert.equal(f.instances[0].destroyed, false);
  cleanup();
});

test('hiding the current final target closes the tour', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].drive(2);
  f.detail.hidden = true;
  f.observers[0].callback();
  assert.equal(f.instances[0].destroyed, true);
  assert.equal(f.root.inert, false);
  cleanup();
});

test('navigation closes the tour, restores focus and releases observer', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  window.dispatchEvent(new Event('vaadin-navigated'));
  assert.equal(f.instances[0].destroyed, true);
  assert.equal(f.root.inert, false);
  assert.equal(f.trigger.focused, true);
  assert.equal(f.observers[0].connected, false);
  cleanup();
});

test('cleanup removes launch handler and supports fresh attachment', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  cleanup();
  f.root.dispatchEvent(new Event('click'));
  assert.equal(f.instances.length, 0);
  const reattached = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  assert.equal(f.instances.length, 1);
  reattached();
});

test('blocks page-wide Enter shortcut while retaining native button activation', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  let calls = 0;
  window.addEventListener('keydown', () => calls++);
  const event = Object.assign(new Event('keydown', {cancelable: true}), {key: 'Enter'});
  window.dispatchEvent(event);
  assert.equal(calls, 0);
  assert.equal(event.defaultPrevented, false);
  cleanup();
});

test('block-guide arrows do not close its single explanation', () => {
  const f = fixture();
  f.trigger.dataset.guideStart = 'block';
  f.block.querySelectorAll = () => [];
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  let reachedLibrary = false;
  window.addEventListener('keyup', () => { reachedLibrary = true; });
  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowRight'}));
  assert.equal(reachedLibrary, false);
  assert.equal(f.instances[0].destroyed, false);
  assert.deepEqual(f.instances[0].config.showButtons, ['close']);
  cleanup();
});

test('keeps an already inert page inert after cleanup', () => {
  const f = fixture();
  f.root.inert = true;
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  cleanup();
  assert.equal(f.root.inert, true);
});

test('refreshes the spotlight geometry when loaded block content changes', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].drive(1);
  f.observers[0].callback();
  assert.equal(f.instances[0].refreshes, 1);
  assert.equal(f.instances[0].getActiveIndex(), 1);
  cleanup();
});

test('should ignore repeated arrows and button commands until the spotlight finishes moving', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].finish();

  f.instances[0].config.onNextClick();
  f.instances[0].config.onNextClick();
  f.instances[0].config.onPrevClick();
  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowRight'}));

  assert.deepEqual(f.instances[0].drives, [0, 1]);
  f.instances[0].finish();
  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowLeft'}));
  assert.equal(f.instances[0].getActiveIndex(), 0);
  cleanup();
});

test('should defer geometry refresh until Driver commits the new spotlight', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].finish();
  f.instances[0].config.onNextClick();

  f.observers[0].callback();

  assert.equal(f.instances[0].refreshes, 0);
  f.instances[0].finish();
  assert.equal(f.instances[0].refreshes, 0);
  f.flushFrame();
  assert.equal(f.instances[0].refreshes, 1);
  cleanup();
});

test('should skip a target that disappears during the spotlight transition', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].finish();
  f.instances[0].config.onNextClick();

  f.block.hidden = true;
  f.observers[0].callback();
  f.instances[0].finish();
  f.flushFrame();

  assert.equal(f.instances[0].getActiveIndex(), 2);
  cleanup();
});

test('should restore inert and focus on Escape even before the first highlighted hook', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);
  // Driver 1.8 omits onDestroyed before its first completed highlight.
  f.instances[0].destroy = function () { this.destroyed = true; };

  window.dispatchEvent(Object.assign(new Event('keydown'), {key: 'Escape'}));

  assert.equal(f.instances[0].destroyed, true);
  assert.equal(f.root.inert, false);
  assert.equal(f.trigger.focused, true);
  assert.equal(f.observers[0].connected, false);
  assert.equal(f.frames.size, 0);
  cleanup();
});

test('should return focus to the launch button after Driver restores a different original focus', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  const focus = [];
  f.trigger.focus = () => focus.push('launch');
  f.instances[0].destroy = function () {
    this.destroyed = true;
    this.config.onDestroyed();
    focus.push('previously focused element');
  };

  f.instances[0].config.onCloseClick();

  assert.deepEqual(focus, ['previously focused element', 'launch']);
  assert.equal(f.root.inert, false);
  cleanup();
});

test('should cancel card motion scheduled before its first frame on navigation', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);

  window.dispatchEvent(new Event('vaadin-navigated'));
  f.flushFrame();

  assert.equal(f.popover.wrapper.animations.length, 0);
  assert.equal(f.popover.wrapper.style.opacity, '');
  assert.equal(f.root.inert, false);
  cleanup();
});

test('should keep old hooks from closing or mutating a restarted guide', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  const old = f.instances[0];
  old.config.onPopoverRender(f.popover);
  f.root.dispatchEvent(new Event('click'));

  old.config.onHighlighted();
  old.config.onDestroyed();
  old.config.onCloseClick();
  old.config.onNextClick();
  old.config.onPopoverRender(f.popover);
  f.flushFrame();

  assert.equal(f.root.inert, true);
  assert.equal(f.instances[1].destroyed, false);
  assert.equal(f.observers[2].connected, true);
  assert.equal(f.popover.wrapper.animations.length, 0);
  cleanup();
});

test('should disable spotlight, card motion and smooth scrolling from launch when reduced motion is enabled', () => {
  const f = fixture({reduced: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].config.onPopoverRender(f.popover);

  assert.equal(f.instances[0].config.animate, false);
  assert.equal(f.instances[0].config.smoothScroll, false);
  assert.equal(f.frames.size, 0);
  assert.equal(f.popover.wrapper.animations.length, 0);
  cleanup();
});

test('should cancel the card animation and complete the current step immediately when reduced motion is enabled', () => {
  const f = fixture({pending: true});
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.instances[0].finish();
  f.instances[0].config.onNextClick();
  f.instances[0].config.onPopoverRender(f.popover);
  f.flushFrame();
  f.motion.matches = true;

  f.motion.dispatchEvent(new Event('change'));

  assert.equal(f.popover.wrapper.animations[0].canceled, true);
  assert.equal(f.popover.wrapper.style.opacity, '');
  assert.deepEqual(f.instances[0].drives, [0, 1, 1]);
  assert.equal(f.instances[0].config.animate, false);
  assert.equal(f.instances[0].config.smoothScroll, false);
  cleanup();
});

test('should restore spotlight animation and smooth scrolling when reduced motion is disabled during a guide', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  f.motion.matches = true;
  f.motion.dispatchEvent(new Event('change'));
  f.motion.matches = false;

  f.motion.dispatchEvent(new Event('change'));

  assert.equal(f.instances[0].config.animate, true);
  assert.equal(f.instances[0].config.smoothScroll, true);
  cleanup();
});

test('should ignore reduced motion changes after cleanup', () => {
  const f = fixture();
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  cleanup();
  f.motion.matches = true;

  f.motion.dispatchEvent(new Event('change'));

  assert.equal(f.instances[0].config.animate, true);
  assert.equal(f.instances[0].config.smoothScroll, true);
});
