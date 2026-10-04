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
