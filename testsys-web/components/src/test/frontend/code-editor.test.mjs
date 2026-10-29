import assert from 'node:assert/strict';
import test from 'node:test';
globalThis.window = {};
globalThis.HTMLTextAreaElement = class {};
await import('../../main/resources/META-INF/frontend/testsys-ui/code-editor.ts');

function textarea({offsetHeight = 200, clientHeight = 200, scrollTop = 0} = {}) {
  const listeners = {};
  return {
    value: 'a\nb\nc', offsetHeight, clientHeight, scrollTop,
    addEventListener(type, listener) { listeners[type] = listener; },
    removeEventListener(type) { delete listeners[type]; },
    fire(type) { listeners[type](); },
  };
}

function numbers() {
  return {textContent: '', scrollTop: 0, style: {}};
}

test('adds the height of a horizontal scrollbar below the line numbers', () => {
  const input = textarea({offsetHeight: 215, clientHeight: 200});
  const lines = numbers();

  window.testsysCodeEditor.attach({inputElement: input}, lines);

  assert.equal(lines.style.paddingBottom, 'calc(var(--ts-code-padding-y) + 15px)');
});

test('keeps the ordinary bottom padding without a horizontal scrollbar', () => {
  const lines = numbers();

  window.testsysCodeEditor.attach({inputElement: textarea()}, lines);

  assert.equal(lines.style.paddingBottom, 'calc(var(--ts-code-padding-y) + 0px)');
});

test('scrolls the line numbers with the text', () => {
  const input = textarea();
  const lines = numbers();
  window.testsysCodeEditor.attach({inputElement: input}, lines);
  input.scrollTop = 120;

  input.fire('scroll');

  assert.equal(lines.scrollTop, 120);
});

test('numbers every line of the text', () => {
  const lines = numbers();

  window.testsysCodeEditor.attach({inputElement: textarea()}, lines);

  assert.equal(lines.textContent, '1\n2\n3');
});
