import assert from 'node:assert/strict';
import test from 'node:test';
import {currentDrag, sortableOrder} from '../../main/resources/META-INF/frontend/testsys-ui/sortable-state.ts';
const key = item => item.key;
test('drag belongs to an unchanged enabled snapshot', () => {
  const items = [{key:'a'}, {key:'b'}], drag = {key:'a', items, from:0, index:1};
  assert.equal(currentDrag(drag, items, false, key), drag);
  assert.deepEqual(sortableOrder(items, drag, key), [items[1], items[0]]);
  for (const replacement of [[], [items[1]], [...items]]) {
    assert.equal(currentDrag(drag, replacement, false, key), null);
    assert.equal(sortableOrder(replacement, drag, key), replacement);
  }
  assert.equal(currentDrag(drag, items, true, key), null);
});
