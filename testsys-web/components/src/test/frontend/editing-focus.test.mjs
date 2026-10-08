import assert from 'node:assert/strict';
import test from 'node:test';
globalThis.window = {};
globalThis.getComputedStyle = node => ({visibility: node.visibility ?? 'visible'});
const {focusFirstEditableInput, isEligible} =
  await import('../../main/resources/META-INF/frontend/testsys-ui/editing-focus.ts');

/** Nodes of one test that record the name of the node that took the focus. */
function fixture() {
  const recorder = {focused: null};

  function node(name, {parts = [], shown = true, hidden = false, visibility, disabled = false, readOnly = false} = {}) {
    return {
      name, disabled, readOnly, visibility, isConnected: true,
      getClientRects: () => (shown ? [{}] : []),
      closest: selector => (selector.startsWith('[hidden]') && hidden ? {} : null),
      querySelectorAll: () => parts,
      focus() { recorder.focused = name; },
    };
  }

  function block(fields, fallback = node('cancel')) {
    const body = {querySelectorAll: () => fields};
    fallback.closest = selector => (selector === '.ts-block' ? {querySelector: () => body} : null);
    return fallback;
  }

  return {recorder, node, block};
}

test('focuses the first editable field of the body', () => {
  const {recorder, node, block} = fixture();
  focusFirstEditableInput(block([node('login'), node('name')]));
  assert.equal(recorder.focused, 'login');
});

test('focuses the first available part of a compound field', () => {
  const {recorder, node, block} = fixture();
  const range = node('range', {parts: [node('from', {disabled: true}), node('to')]});
  focusFirstEditableInput(block([range]));
  assert.equal(recorder.focused, 'to');
});

test('skips hidden, invisible, disabled and read-only fields', () => {
  const {recorder, node, block} = fixture();
  focusFirstEditableInput(block([
    node('collapsed', {shown: false}),
    node('inside-hidden', {hidden: true}),
    node('invisible', {visibility: 'hidden'}),
    node('disabled', {disabled: true}),
    node('read-only', {readOnly: true}),
    node('editable'),
  ]));
  assert.equal(recorder.focused, 'editable');
});

test('falls back to the cancel action without an editable field', () => {
  const {recorder, node, block} = fixture();
  focusFirstEditableInput(block([node('read-only', {readOnly: true})]));
  assert.equal(recorder.focused, 'cancel');
});

test('does not focus a hidden fallback', () => {
  const {node} = fixture();
  assert.equal(isEligible(node('cancel', {shown: false})), false);
});
