import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function PillTabs({ items = [], value, onChange, className }) {
  return (
    <div className={cx('ts-pills', className)}>
      {items.map(it => { const o = typeof it === 'string' ? { value: it, label: it } : it; return <button type="button" key={o.value} className={cx('ts-pill', o.value === value && 'ts-pill--active')} onClick={() => onChange && onChange(o.value)}>{o.label}</button>; })}
    </div>
  );
}
