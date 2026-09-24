import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Tag({ variant = 'default', onRemove, children, className }) {
  return (
    <span className={cx('ts-tag', variant !== 'default' && 'ts-tag--' + variant, className)}>
      {children}
      {onRemove ? <button type="button" onClick={onRemove} aria-label="Убрать" style={{ border: 0, background: 'none', padding: 0, cursor: 'pointer', color: 'var(--text-secondary)', display: 'grid' }}><Icon name="x" size={12} strokeWidth={2.5} /></button> : null}
    </span>
  );
}
