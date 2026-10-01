import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Switch({ checked = false, onChange, label, disabled = false, className, id, ...rest }) {
  const sw = <span aria-hidden="true" aria-checked={checked} className={cx('ts-switch', checked && 'ts-switch--on')}><span className="ts-switch__knob" /></span>;
  if (!label) return <span id={id} {...rest} role="switch" aria-label="Переключить" aria-checked={checked} aria-disabled={disabled} tabIndex={disabled ? -1 : 0} onKeyDown={e => { if (['Enter', ' '].includes(e.key)) { e.preventDefault(); !disabled && onChange?.(!checked); } }} style={{ display: 'inline-flex', cursor: 'pointer' }} className={className} onClick={() => !disabled && onChange && onChange(!checked)}>{sw}</span>;
  return (
    <span id={id} {...rest} role="switch" aria-checked={checked} aria-disabled={disabled} tabIndex={disabled ? -1 : 0} onKeyDown={e => { if (['Enter', ' '].includes(e.key)) { e.preventDefault(); !disabled && onChange?.(!checked); } }} className={cx('ts-choice', disabled && 'ts-choice--disabled', className)} style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }} onClick={() => !disabled && onChange && onChange(!checked)}>
      <span>{label}</span>{sw}
    </span>
  );
}
