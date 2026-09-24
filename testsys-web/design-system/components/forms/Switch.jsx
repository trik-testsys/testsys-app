import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Switch({ checked = false, onChange, label, disabled = false, className }) {
  const sw = <span role="switch" aria-checked={checked} className={cx('ts-switch', checked && 'ts-switch--on')}><span className="ts-switch__knob" /></span>;
  if (!label) return <span style={{ display: 'inline-flex', cursor: 'pointer' }} className={className} onClick={() => !disabled && onChange && onChange(!checked)}>{sw}</span>;
  return (
    <span className={cx('ts-choice', disabled && 'ts-choice--disabled', className)} style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }} onClick={() => !disabled && onChange && onChange(!checked)}>
      <span>{label}</span>{sw}
    </span>
  );
}
