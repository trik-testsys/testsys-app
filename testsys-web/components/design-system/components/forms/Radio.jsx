import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Radio({ checked = false, onChange, label, disabled = false, className, id, ...rest }) {
  return (
    <span id={id} {...rest} tabIndex={disabled ? -1 : 0} aria-disabled={disabled} onKeyDown={e => { if (['Enter', ' '].includes(e.key)) { e.preventDefault(); !disabled && onChange?.(true); } }} role="radio" aria-checked={checked} className={cx('ts-choice', disabled && 'ts-choice--disabled', className)} onClick={() => !disabled && onChange && onChange(true)}>
      <span className={cx('ts-radio', checked && 'ts-radio--on')}>{checked ? <span className="ts-radio__dot" /> : null}</span>{label}
    </span>
  );
}
