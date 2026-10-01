import { test } from 'node:test';
import assert from 'node:assert/strict';
import { getPopoverBounds } from '../lib/popover-geometry.mjs';

test('should align the user panel to its trigger right edge', () => {
  assert.deepEqual(getPopoverBounds({ width: 1440, right: 1440 }, { right: 1428 }), { right: 12, width: 320 });
});
test('should align the bell independently from the user trigger', () => {
  assert.deepEqual(getPopoverBounds({ width: 1440, right: 1440 }, { right: 1290 }), { right: 150, width: 320 });
});
test('should use header-relative coordinates when the header is inset', () => {
  assert.deepEqual(getPopoverBounds({ width: 1440, right: 1540 }, { right: 1500 }), { right: 40, width: 320 });
});
test('should clamp a panel at the left viewport edge', () => {
  assert.deepEqual(getPopoverBounds({ width: 1440, right: 1440 }, { right: 50 }), { right: 1108, width: 320 });
});
test('should shrink the panel to a narrow header preserving both insets', () => {
  assert.deepEqual(getPopoverBounds({ width: 200, right: 200 }, { right: 188 }), { right: 12, width: 176 });
});
test('should preserve matching right edges after a desktop resize', () => {
  assert.deepEqual(getPopoverBounds({ width: 1920, right: 1920 }, { right: 1908 }), { right: 12, width: 320 });
});
