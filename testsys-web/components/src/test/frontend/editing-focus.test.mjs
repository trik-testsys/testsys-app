import assert from 'node:assert/strict';
import test from 'node:test';
globalThis.window = {};
globalThis.getComputedStyle = node => ({visibility: node.visibility ?? 'visible'});
const {focusFirstEditableInput, isEligible} =
  await import('../../main/resources/META-INF/frontend/testsys-ui/editing-focus.ts');

let focused = null;

function node(name, {parts = [], shown = true, hidden = false, visibility, disabled = false, readOnly = false} = {}) {
  return {
    name, disabled, readOnly, visibility, isConnected: true,
    getClientRects: () => (shown ? [{}] : []),
    closest: selector => (selector.startsWith('[hidden]') && hidden ? {} : null),
    querySelectorAll: () => parts,
    focus() { focused = name; },
  };
}

function block(fields, fallback = node('cancel')) {
  const body = {querySelectorAll: () => fields};
  fallback.closest = selector => (selector === '.ts-block' ? {querySelector: () => body} : null);
  return fallback;
}

test('focuses the first editable field of the body', () => {
  focused = null;
  focusFirstEditableInput(block([node('login'), node('name')]));
  assert.equal(focused, 'login');
});

test('focuses the first available part of a compound field', () => {
  focused = null;
  const range = node('range', {parts: [node('from', {disabled: true}), node('to')]});
  focusFirstEditableInput(block([range]));
  assert.equal(focused, 'to');
});

test('skips hidden, invisible, disabled and read-only fields', () => {
  focused = null;
  focusFirstEditableInput(block([
    node('collapsed', {shown: false}),
    node('inside-hidden', {hidden: true}),
    node('invisible', {visibility: 'hidden'}),
    node('disabled', {disabled: true}),
    node('read-only', {readOnly: true}),
    node('editable'),
  ]));
  assert.equal(focused, 'editable');
});

test('falls back to the cancel action without an editable field', () => {
  focused = null;
  focusFirstEditableInput(block([node('read-only', {readOnly: true})]));
  assert.equal(focused, 'cancel');
});

test('does not focus a hidden fallback', () => {
  assert.equal(isEligible(node('cancel', {shown: false})), false);
});
