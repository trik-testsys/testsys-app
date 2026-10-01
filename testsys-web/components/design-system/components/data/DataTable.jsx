import React from 'react';
import { Checkbox } from '../forms/Checkbox.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');
const SEMANTIC_WIDTHS = ['narrow', 'medium', 'wide', 'fill'];

export function DataTable({ columns = [], rows = [], rowKey = 'id', selectable = false, selected = [], onSelectChange, sort, onSortChange, compact = false, onRowClick, empty, newKeys = [] }) {
  const keys = rows.map((r, i) => r[rowKey] != null ? r[rowKey] : i);
  const all = keys.length > 0 && keys.every(k => selected.includes(k));
  const some = !all && keys.some(k => selected.includes(k));
  const toggle = k => onSelectChange && onSelectChange(selected.includes(k) ? selected.filter(x => x !== k) : [...selected, k]);
  return (
    <table className={cx('ts-table', compact && 'ts-table--compact')}>
      <thead>
        <tr>
          {selectable ? <th style={{ width: 52 }}><Checkbox checked={all} indeterminate={some} onChange={() => onSelectChange && onSelectChange(all ? [] : keys)} /></th> : null}
          {columns.map(c => {
            const sorted = sort && sort.key === c.key;
            const semantic = SEMANTIC_WIDTHS.includes(c.width);
            return (
              <th aria-sort={sorted ? sort.dir === 'asc' ? 'ascending' : 'descending' : undefined} tabIndex={c.sortable ? 0 : undefined} onKeyDown={e => { if (c.sortable && ['Enter', ' '].includes(e.key)) { e.preventDefault(); onSortChange?.({ key: c.key, dir: sorted && sort.dir === 'desc' ? 'asc' : 'desc' }); } }} key={c.key} style={semantic ? undefined : { width: c.width }} className={cx(c.align && 'ts-' + c.align, semantic && 'ts-col--' + c.width, c.sortable && 'ts-sortable', sorted && 'ts-sorted')}
                onClick={c.sortable && onSortChange ? () => onSortChange({ key: c.key, dir: sorted && sort.dir === 'desc' ? 'asc' : 'desc' }) : undefined}>
                {c.title}{sorted ? (sort.dir === 'desc' ? ' ↓' : ' ↑') : ''}
              </th>
            );
          })}
        </tr>
      </thead>
      <tbody>
        {rows.length === 0 && empty ? <tr><td colSpan={columns.length + (selectable ? 1 : 0)} style={{ padding: 0 }}>{empty}</td></tr> : null}
        {rows.map((r, i) => {
          const k = keys[i], on = selected.includes(k);
          return (
            <tr key={k} className={cx(on && 'ts-row-selected', newKeys.includes(k) && 'ts-row-new') || undefined} onClick={onRowClick ? () => onRowClick(r) : undefined} style={onRowClick ? { cursor: 'pointer' } : undefined}>
              {selectable ? <td><Checkbox checked={on} onChange={() => toggle(k)} /></td> : null}
              {columns.map(c => <td key={c.key} className={cx(c.align && 'ts-' + c.align, c.mono && 'ts-num')} style={c.muted ? { color: 'var(--text-secondary)' } : undefined}>{c.render ? c.render(r) : r[c.key]}</td>)}
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
