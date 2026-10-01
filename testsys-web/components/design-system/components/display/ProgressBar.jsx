import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function ProgressBar({ value = 0, tone = 'accent', thin = false, indeterminate = false, inverse = false, className, style }) {
  return (
    <div className={cx('ts-progress', thin && 'ts-progress--thin', tone !== 'accent' && 'ts-progress--' + tone, indeterminate && 'ts-progress--indeterminate', inverse && 'ts-progress--inverse', className)} style={style}
      role="progressbar" aria-valuenow={indeterminate ? undefined : Math.round(value)} aria-valuemin={0} aria-valuemax={100}>
      <div className="ts-progress__bar" style={indeterminate ? undefined : { width: Math.max(0, Math.min(100, value)) + '%' }} />
    </div>
  );
}
