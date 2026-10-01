import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Button({ variant = 'primary', size = 'md', icon, iconRight, loading = false, block = false, disabled = false, className, children, onClick, type = 'button', ...rest }) {
  const iconSize = size === 'sm' ? 14 : 16;
  return (
    <button type={type} className={cx('ts-btn', 'ts-btn--' + variant, size !== 'md' && 'ts-btn--' + size, block && 'ts-btn--block', className)}
      disabled={disabled} aria-busy={loading || undefined} onClick={loading ? undefined : onClick} {...rest}>
      {loading ? <span className="ts-spinner" /> : icon ? <Icon name={icon} size={iconSize} /> : null}
      {children}
      {iconRight ? <Icon name={iconRight} size={14} /> : null}
    </button>
  );
}
