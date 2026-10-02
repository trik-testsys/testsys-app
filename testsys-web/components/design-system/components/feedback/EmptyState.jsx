import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function EmptyState({ icon = 'file', title, description, action, error = false }) {
  return (
    <div className={cx('ts-empty', error && 'ts-empty--error')}>
      <span className="ts-empty__icon"><Icon name={error ? 'triangle-alert' : icon} size={22} strokeWidth={1.8} /></span>
      <span className="ts-empty__title">{title}</span>
      {description ? <span className="ts-empty__desc">{description}</span> : null}
      {action ? <div style={{ marginTop: 4 }}>{action}</div> : null}
    </div>
  );
}
