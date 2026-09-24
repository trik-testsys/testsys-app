import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function StatusBadge({ tone = 'neutral', size = 'md', dot = true, children, className }) {
  return <span className={cx('ts-status', 'ts-status--' + tone, size === 'sm' && 'ts-status--sm', className)}>{dot && tone !== 'draft' ? <span className="ts-status__dot" /> : null}{children}</span>;
}
