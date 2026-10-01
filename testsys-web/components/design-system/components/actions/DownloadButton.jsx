import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

function Ring({ p }) {
  const c = 81.7;
  return (
    <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r="13" fill="none" stroke="var(--muted)" strokeWidth="3" />
      <circle cx="16" cy="16" r="13" fill="none" stroke="var(--accent)" strokeWidth="3" strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - p / 100)} transform="rotate(-90 16 16)" />
      <rect x="12.5" y="12.5" width="7" height="7" rx="1.5" fill="var(--accent)" />
    </svg>
  );
}

export function DownloadButton({ state = 'idle', progress = 0, size = 'md', iconOnly = false, onClick, onCancel, labels = {}, className }) {
  const L = { idle: 'Скачать', preparing: 'Подготовка…', cancel: 'Отмена', done: 'Скачано', error: 'Повторить', ...labels };
  const sz = size !== 'md' && 'ts-btn--' + size;
  const p = Math.round(progress);
  if (iconOnly) {
    if (state === 'downloading') return <button type="button" aria-label={L.cancel + ' ' + p + '%'} onClick={onCancel} className={cx('ts-btn ts-btn--icon ts-btn--ghost', sz, className)}><Ring p={p} /></button>;
    const v = { idle: 'secondary', preparing: 'busy', done: 'success-soft', error: 'danger-soft' }[state];
    const ic = { idle: 'download', done: 'check', error: 'refresh-cw' }[state];
    return <button type="button" aria-label={L[state] || L.idle} onClick={state === 'preparing' ? onCancel : onClick} className={cx('ts-btn ts-btn--icon', 'ts-btn--' + v, sz, className)}>{state === 'preparing' ? <span className="ts-spinner" /> : <Icon name={ic} size={16} />}</button>;
  }
  if (state === 'preparing') return <button type="button" onClick={onCancel} className={cx('ts-btn ts-btn--busy', sz, className)}><span className="ts-spinner" />{L.preparing}</button>;
  if (state === 'downloading') return (
    <button type="button" onClick={onCancel} className={cx('ts-btn ts-btn--progress', sz, className)} style={{ minWidth: 128 }}>
      <span className="ts-btn__fill" style={{ width: p + '%' }} />
      <span className="ts-btn__label"><span className="ts-mono">{p}%</span>{L.cancel}</span>
    </button>
  );
  if (state === 'done') return <button type="button" onClick={onClick} className={cx('ts-btn ts-btn--success-soft', sz, className)}><Icon name="check" size={16} />{L.done}</button>;
  if (state === 'error') return <button type="button" onClick={onClick} className={cx('ts-btn ts-btn--danger-soft', sz, className)}><Icon name="refresh-cw" size={14} />{L.error}</button>;
  return <button type="button" onClick={onClick} className={cx('ts-btn ts-btn--secondary', sz, className)}><Icon name="download" size={16} />{L.idle}</button>;
}
