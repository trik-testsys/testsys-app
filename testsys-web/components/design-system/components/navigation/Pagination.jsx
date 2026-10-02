import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

function range(page, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (page <= 4) return [1, 2, 3, 4, 5, '…', total];
  if (page >= total - 3) return [1, '…', total - 4, total - 3, total - 2, total - 1, total];
  return [1, '…', page - 1, page, page + 1, '…', total];
}
export function Pagination({ page = 1, total = 1, onChange, compact = false }) {
  const go = p => onChange && onChange(Math.max(1, Math.min(total, p)));
  const prev = <button type="button" className="ts-pager__btn" aria-label="Назад" onClick={() => go(page - 1)}><Icon name="chevron-left" size={14} /></button>;
  const next = <button type="button" className="ts-pager__btn" aria-label="Вперёд" onClick={() => go(page + 1)}><Icon name="chevron-right" size={14} /></button>;
  if (compact) return <div className="ts-pager">{prev}<span className="ts-pager__label">{page} / {total}</span>{next}</div>;
  return (
    <div className="ts-pager">
      {prev}
      {range(page, total).map((p, i) => p === '…' ? <span key={'g' + i} className="ts-pager__gap">…</span> : <button type="button" key={p} className={cx('ts-pager__btn', p === page && 'ts-pager__btn--active')} onClick={() => go(p)}>{p}</button>)}
      {next}
    </div>
  );
}
