import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createTableState, updateTableState, filterTableRows, paginateTableRows, prototypeDate } from '../ui_kits/platform/PrototypeTables.mjs';

const defaults = { query: '', category: 'all', from: '', to: '' };
test('should keep applied filters unchanged while editing the draft', () => {
  const state = updateTableState(createTableState(defaults), { type: 'draft', name: 'query', value: 'Анна' }, defaults);
  assert.equal(state.draft.query, 'Анна');
  assert.equal(state.applied.query, '');
});
test('should apply a valid draft and return to the first page', () => {
  const state = { ...createTableState(defaults), draft: { ...defaults, query: 'Анна' }, page: 3 };
  const applied = updateTableState(state, { type: 'apply' }, defaults);
  assert.equal(applied.applied.query, 'Анна');
  assert.equal(applied.page, 1);
});
test('should reject a reversed period without changing applied filters or page', () => {
  const state = { ...createTableState(defaults), draft: { ...defaults, from: '2026-10-03', to: '2026-10-01' }, page: 2 };
  const rejected = updateTableState(state, { type: 'apply' }, defaults);
  assert.deepEqual(rejected.applied, defaults);
  assert.equal(rejected.page, 2);
  assert.ok(rejected.errors.period);
});
test('should restore defaults and clear errors on reset', () => {
  const state = { ...createTableState(defaults), draft: { ...defaults, query: 'changed' }, page: 3, errors: { period: 'bad' } };
  assert.deepEqual(updateTableState(state, { type: 'reset' }, defaults), createTableState(defaults));
});
test('should filter only the supplied scoped records using applied values', () => {
  const scoped = [{ id: 'a', name: 'Анна', category: 'Checked', date: '01.10.2026 10:00' }, { id: 'b', name: 'Иван', category: 'Queue', date: '02.10.2026 11:00' }];
  const rows = filterTableRows(scoped, { query: 'ан', category: 'Checked', from: '2026-10-01', to: '2026-10-01' }, { searchText: row => row.name, categoryValue: row => row.category, dateValue: row => row.date });
  assert.deepEqual(rows.map(row => row.id), ['a']);
});
test('should clamp pagination after a scoped dataset shrinks', () => {
  assert.deepEqual(paginateTableRows([1, 2, 3], 4, 2), { rows: [3], page: 2, pageCount: 2, total: 3 });
  assert.deepEqual(paginateTableRows([], 3, 10), { rows: [], page: 1, pageCount: 1, total: 0 });
});
test('should normalize prototype dates without treating absent dates as real values', () => {
  assert.equal(prototypeDate('01.10.2026 10:00'), '2026-10-01');
  assert.equal(prototypeDate('2026-10-02'), '2026-10-02');
  assert.equal(prototypeDate('—'), '');
});

for (const date of ['bad', '2026-02-29', '2026-13-01', '2026-00-10', '2026-01-00', '2026-10-02-extra']) {
  test(`should reject an invalid manually entered date ${date} without applying`, () => {
    const state = { ...createTableState(defaults), draft: { ...defaults, from: date }, page: 2 };
    const rejected = updateTableState(state, { type: 'apply' }, defaults);
    assert.deepEqual(rejected.applied, defaults);
    assert.equal(rejected.page, 2);
    assert.ok(rejected.errors.period);
  });
}
test('should accept a real leap day and a partial date range', () => {
  const state = { ...createTableState(defaults), draft: { ...defaults, from: '2024-02-29', to: '' } };
  const applied = updateTableState(state, { type: 'apply' }, defaults);
  assert.equal(applied.applied.from, '2024-02-29');
  assert.deepEqual(applied.errors, {});
});
