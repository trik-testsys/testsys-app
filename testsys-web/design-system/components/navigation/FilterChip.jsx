import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function FilterChip({ selected = false, onChange, dropdown = false, children, className }) {
  return (
    <button type="button" aria-pressed={selected} className={cx('ts-filter', selected && 'ts-filter--on', className)} onClick={() => onChange && onChange(!selected)}>
      {selected && !dropdown ? <Icon name="check" size={12} strokeWidth={3} /> : null}
      {children}
      {dropdown ? <Icon name="chevron-down" size={14} /> : null}
    </button>
  );
}
