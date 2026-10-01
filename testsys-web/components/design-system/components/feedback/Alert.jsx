import React from 'react';
import { Icon } from '../core/Icon.jsx';
const ICON = { info: 'info', warning: 'triangle-alert', danger: 'circle-x', success: 'circle-check' };
export function Alert({ tone = 'info', title, children, action, badge }) {
  if (tone === 'dark') return (
    <div className="ts-alert ts-alert--dark">
      {badge ? <span style={{ font: '600 12px var(--font-sans)', padding: '3px 8px', borderRadius: 999, background: 'var(--accent)' }}>{badge}</span> : null}
      <span style={{ flex: 1, font: '500 14px var(--font-sans)' }}>{title}</span>{action}
    </div>
  );
  return (
    <div className={'ts-alert ts-alert--' + tone} role={tone === 'danger' ? 'alert' : undefined}>
      <Icon name={ICON[tone]} size={18} className="ts-alert__icon" />
      <div className="ts-alert__text">{title ? <span className="ts-alert__title">{title}</span> : null}{children ? <span className="ts-alert__desc">{children}</span> : null}</div>
      {action}
    </div>
  );
}
