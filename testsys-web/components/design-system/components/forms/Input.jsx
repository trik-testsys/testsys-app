import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Input({ size = 'md', error = false, mono = false, prefix, suffix, className, style, ...rest }) {
  if (prefix || suffix) {
    return (
      <div className={cx('ts-affix', className)} style={{ borderColor: error ? 'var(--danger)' : undefined, ...style }}>
        {prefix ? <span className="ts-affix__pre">{prefix}</span> : null}
        <input className={mono ? 'ts-mono' : undefined} {...rest} />
        {suffix ? <span className="ts-affix__post">{suffix}</span> : null}
      </div>
    );
  }
  return <input className={cx('ts-input', size === 'lg' && 'ts-input--lg', error && 'ts-input--error', mono && 'ts-input--mono', className)} style={style} {...rest} />;
}
