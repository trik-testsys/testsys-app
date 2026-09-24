import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Field({ label, hint, error, aside, disabled = false, children, className, style }) {
  return (
    <div className={cx('ts-field', className)} style={style}>
      {label ? <span className={cx('ts-label', disabled && 'ts-label--disabled')}><span>{label}</span>{aside}</span> : null}
      {children}
      {error ? <span className="ts-error">{error}</span> : hint ? <span className="ts-hint">{hint}</span> : null}
    </div>
  );
}
