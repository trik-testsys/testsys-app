import assert from 'node:assert/strict';
import test from 'node:test';
import {calendarIso, civilDate} from '../../main/resources/META-INF/frontend/testsys-ui/calendar-date.ts';

test('civil years below 100 preserve the selected year', () => {
  assert.equal(civilDate(99, 0, 1).getUTCFullYear(), 99);
  assert.equal(calendarIso(99, 0, 1), '0099-01-01');
});

test('month navigation uses civil leap days without local timezone conversion', () => {
  assert.equal(civilDate(2024, 2, 0).getUTCDate(), 29);
  assert.equal(civilDate(2026, 2, 0).getUTCDate(), 28);
  assert.equal(civilDate(2026, -1, 31).getUTCFullYear(), 2025);
  assert.equal(civilDate(2026, 12, 1).getUTCMonth(), 0);
});

test('ISO boundaries retain signed years and literal calendar months', () => {
  assert.equal(calendarIso(-1, 11, 31), '-0001-12-31');
  assert.equal(calendarIso(10000, 0, 1), '+10000-01-01');
  assert.equal(calendarIso(2026, 9, 2), '2026-10-02');
});

const behavior = await import('../../main/resources/META-INF/frontend/testsys-ui/calendar-date.ts');
test('signed civil ranges complete and highlight chronologically', () => {
  for (const [start, day, end] of [
    ['-0002-12-30', '-0002-12-31', '-0001-01-01'],
    ['9999-12-30', '9999-12-31', '+10000-01-01'],
    ['2026-10-01', '2026-10-02', '2026-10-03'],
  ]) {
    assert.deepEqual(behavior.pickCalendarDate(start, null, end), {start, end});
    assert.equal(behavior.inCalendarRange(day, start, end), true);
    assert.equal(behavior.inCalendarRange(start, start, end), false);
    assert.deepEqual(behavior.pickCalendarDate(end, null, start), {start, end:null});
    assert.deepEqual(behavior.pickCalendarDate(start, end, day), {start:day, end:null});
  }
});
