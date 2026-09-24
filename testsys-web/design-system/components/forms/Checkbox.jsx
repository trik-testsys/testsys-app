import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Checkbox({ checked = false, indeterminate = false, onChange, label, disabled = false, className }) {
  const on = checked || indeterminate;
  const box = (
    <span role="checkbox" aria-checked={indeterminate ? 'mixed' : checked} className={cx('ts-check', on && 'ts-check--on')}>
      {indeterminate ? <span className="ts-check__dash" /> : checked ? <Icon name="check" size={12} strokeWidth={3.5} /> : null}
    </span>
  );
  if (!label) return onChange ? <span onClick={e => { e.stopPropagation(); !disabled && onChange(!checked); }} style={{ display: 'inline-flex', cursor: 'pointer' }} className={className}>{box}</span> : box;
  return <span className={cx('ts-choice', disabled && 'ts-choice--disabled', className)} onClick={() => !disabled && onChange && onChange(!checked)}>{box}{label}</span>;
}
