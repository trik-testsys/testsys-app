import assert from 'node:assert/strict';
import test from 'node:test';

globalThis.window = {};
globalThis.document = new EventTarget();
await import('../../main/resources/META-INF/frontend/testsys-ui/header-interactions.ts');
const header = window.testsysHeader;

class SearchInput extends EventTarget {
  value = '';
  focused = 0;
  focus() { this.focused++; this.dispatchEvent(new Event('focus')); }
}

function fixture(opened = true) {
  const input = new SearchInput(), trigger = new EventTarget();
  const popup = Object.assign(new EventTarget(), {opened, querySelectorAll: () => []});
  const closed = [], shown = [];
  input.addEventListener('header-menu-close', event => closed.push(event.detail));
  input.addEventListener('header-menu-input', event => shown.push(event.detail));
  header.menuSearchAttach(input, popup, trigger);
  return {input, popup, trigger, closed, shown};
}

function escape(target) {
  const event = new Event('keydown', {cancelable: true});
  Object.defineProperty(event, 'key', {value: 'Escape'});
  target.dispatchEvent(event);
  return event;
}

// Escape from the trigger must close the menu and restore search focus without reopening it.
test('Escape on an open menu trigger closes once and restores search focus', () => {
  const {input, popup, trigger, closed, shown} = fixture();
  const event = escape(trigger);
  assert.equal(popup.opened, false);
  assert.deepEqual(closed, [{}]);
  assert.equal(input.focused, 1);
  assert.deepEqual(shown, []);
  assert.equal(event.defaultPrevented, true);
  header.menuSearchDetach(input);
});

test('Escape on a closed menu trigger preserves focus and sends no close event', () => {
  const {input, popup, trigger, closed} = fixture(false);
  const event = escape(trigger);
  assert.equal(popup.opened, false);
  assert.deepEqual(closed, []);
  assert.equal(input.focused, 0);
  assert.equal(event.defaultPrevented, false);
  header.menuSearchDetach(input);
});

test('repeated attachment keeps one trigger listener and detachment removes it', () => {
  const {input, popup, trigger, closed} = fixture();
  header.menuSearchAttach(input, popup, trigger);
  escape(trigger);
  assert.deepEqual(closed, [{}]);
  assert.equal(input.focused, 1);
  header.menuSearchDetach(input);
  popup.opened = true;
  escape(trigger);
  assert.equal(popup.opened, true);
  assert.deepEqual(closed, [{}]);
  assert.equal(input.focused, 1);
});
