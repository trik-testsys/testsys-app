import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function IconButton({ icon, label, variant = 'ghost', size = 'md', className, type = 'button', ...rest }) {
  return (
    <button type={type} aria-label={label} title={label} className={cx('ts-btn', 'ts-btn--icon', 'ts-btn--' + variant, size !== 'md' && 'ts-btn--' + size, className)} {...rest}>
      <Icon name={icon} size={size === 'lg' ? 18 : 16} />
    </button>
  );
}
