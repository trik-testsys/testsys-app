import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Checkbox({ checked = false, indeterminate = false, onChange, label, disabled = false, className, id, ...rest }) {
  const on = checked || indeterminate;
  const box = <span aria-hidden="true" className={cx('ts-check', on && 'ts-check--on')}>
    {indeterminate ? <span className="ts-check__dash" /> : checked ? <Icon name="check" size={12} strokeWidth={3.5} /> : null}
  </span>;
  if (!label && !onChange && !id) return box;
  return <button id={id} type="button" role="checkbox" aria-checked={indeterminate ? 'mixed' : checked} aria-label={typeof label === 'string' ? label : 'Выбрать'} disabled={disabled} {...rest}
    className={cx(label ? 'ts-choice' : 'ts-choice-control', disabled && 'ts-choice--disabled', className)}
    onClick={e => { e.stopPropagation(); onChange?.(!checked); }}>{box}{label}</button>;
}
