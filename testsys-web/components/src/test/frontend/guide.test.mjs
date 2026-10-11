import assert from 'node:assert/strict';
import test from 'node:test';
import {collectTargets, installGuide, plainText} from '../../main/resources/META-INF/frontend/testsys-ui/guide-controller.ts';

function fixture() {
  globalThis.window = new EventTarget();
  globalThis.getComputedStyle = element => ({visibility: element.visibility ?? 'visible'});
  const observers = [];
  globalThis.MutationObserver = class {
    constructor(callback) { this.callback = callback; observers.push(this); }
    observe() { this.connected = true; }
    disconnect() { this.connected = false; }
  };
  const target = (title, order) => ({
    dataset: {guideTitle: title, guideDescription: '<b>plain</b>', guideOrder: order},
    isConnected: true, hidden: false,
    getClientRects() { return this.hidden ? [] : [{}]; },
    closest() { return this.hidden ? this : null; },
    focus() { this.focused = true; },
  });
  const intro = target('Introduction');
  const detail = target('Detail', '0');
  const block = target('Block');
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
  function factory(config) {
    let index = 0;
    const instance = {
      config, destroyed: false, refreshes: 0,
      refresh() { this.refreshes++; },
      drive(next = 0) { index = next; },
      getActiveIndex() { return index; },
      hasNextStep() { return index < config.steps.length - 1; },
      destroy() { this.destroyed = true; config.onDestroyed(); },
    };
    instances.push(instance);
    return instance;
  }
  const labels = {previousLabel: 'Назад', nextLabel: 'Далее', doneLabel: 'Готово', closeLabel: 'Закрыть', progressLabel: '{{current}} из {{total}}'};
  return {root, intro, block, detail, trigger, labels, factory, instances, observers};
}

test('collects page introduction, block and detailed steps in order', () => {
  const f = fixture();
  assert.deepEqual(collectTargets(f.root, f.trigger), [f.intro, f.block, f.detail]);
});

test('a block launch contains only that block', () => {
  const f = fixture();
  f.trigger.dataset.guideStart = 'block';
  assert.deepEqual(collectTargets(f.root, f.trigger), [f.block]);
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
  assert.equal(f.instances[0].config.animate, false);
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
  const cleanup = installGuide(f.root, f.labels, f.factory);
  f.root.dispatchEvent(new Event('click'));
  let reachedLibrary = false;
  window.addEventListener('keyup', () => { reachedLibrary = true; });
  window.dispatchEvent(Object.assign(new Event('keyup'), {key: 'ArrowRight'}));
  assert.equal(reachedLibrary, false);
  assert.equal(f.instances[0].destroyed, false);
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
