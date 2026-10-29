import assert from 'node:assert/strict';
import test from 'node:test';
globalThis.window = {};
const {restoreFocus, shouldRestoreFocus} = await import('../../main/resources/META-INF/frontend/testsys-ui/popup-focus.ts');

function popup(...inside) {
  return {contains: node => inside.includes(node)};
}

function trigger() {
  return {isFocused: false, focus() { this.isFocused = true; }};
}

test('restores the focus lost to the document body', () => {
  const body = {};

  assert.equal(shouldRestoreFocus(body, popup(), body), true);
});

test('restores the focus left inside the closed popup', () => {
  const option = {};

  assert.equal(shouldRestoreFocus(option, popup(option), {}), true);
});

test('keeps the focus moved to an element outside the popup', () => {
  assert.equal(shouldRestoreFocus({}, popup(), {}), false);
});

test('focuses the trigger when the focus stayed inside the popup', () => {
  const option = {};
  const button = trigger();
  globalThis.document = {activeElement: option, body: {}};

  restoreFocus(button, popup(option));

  assert.equal(button.isFocused, true);
});

test('registers the restore command on the window', () => {
  assert.equal(window.testsysPopupFocus.restore, restoreFocus);
});
