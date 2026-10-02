import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Textarea({ error = false, className, ...rest }) {
  return <textarea className={cx('ts-input', 'ts-textarea', error && 'ts-input--error', className)} {...rest} />;
}
