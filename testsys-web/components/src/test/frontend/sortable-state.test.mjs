import assert from 'node:assert/strict';
import test from 'node:test';
import {currentDrag, sortableOrder} from '../../main/resources/META-INF/frontend/testsys-ui/sortable-state.ts';
const key = item => item.key;
const items = [{key:'a'}, {key:'b'}];
const drag = {key:'a', items, from:0, index:1};
const replacements = {empty: [], shorter: [items[1]], copied: [...items]};

test('drag belongs to an unchanged enabled snapshot', () => {
  assert.equal(currentDrag(drag, items, false, key), drag);
});

test('an unchanged snapshot is shown in the dragged order', () => {
  assert.deepEqual(sortableOrder(items, drag, key), [items[1], items[0]]);
});

for (const [name, replacement] of Object.entries(replacements)) {
  test(`drag does not belong to a ${name} replacement snapshot`, () => {
    assert.equal(currentDrag(drag, replacement, false, key), null);
  });

  test(`a ${name} replacement snapshot keeps its own order`, () => {
    assert.equal(sortableOrder(replacement, drag, key), replacement);
  });
}

test('drag does not belong to a disabled list', () => {
  assert.equal(currentDrag(drag, items, true, key), null);
});
