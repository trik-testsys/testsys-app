import React from 'react';
import { Counter } from '../display/Counter.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Tabs({ items = [], value, onChange, size = 'md', bare = false, className }) {
  return (
    <div role="tablist" className={cx('ts-tabs', size === 'lg' && 'ts-tabs--lg', bare && 'ts-tabs--bare', className)}>
      {items.map(it => {
        const o = typeof it === 'string' ? { value: it, label: it } : it;
        return (
          <button type="button" role="tab" aria-selected={o.value === value} key={o.value} className={cx('ts-tab', o.value === value && 'ts-tab--active')} onClick={() => onChange && onChange(o.value)}>
            {o.label}{o.count != null ? <Counter tone={o.countTone || 'muted'}>{o.count}</Counter> : null}
          </button>
        );
      })}
    </div>
  );
}
