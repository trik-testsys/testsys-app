import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function SegmentedControl({ options = [], value, onChange, size = 'md', block = false, className }) {
  const opts = options.map(o => typeof o === 'string' ? { value: o, label: o } : o);
  return (
    <div role="tablist" className={cx('ts-seg', block && 'ts-seg--block', size !== 'md' && 'ts-seg--' + size, className)}>
      {opts.map(o => (
        <button type="button" role="tab" aria-selected={o.value === value} key={o.value} className={cx('ts-seg__item', o.value === value && 'ts-seg__item--active')} onClick={() => onChange && onChange(o.value)}>{o.label}</button>
      ))}
    </div>
  );
}
