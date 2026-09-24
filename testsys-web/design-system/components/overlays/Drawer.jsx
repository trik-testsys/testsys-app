import React from 'react';
import { IconButton } from '../actions/IconButton.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Drawer({ open = true, title, subtitle, children, footer, onClose, width = 480, contained = false }) {
  if (!open) return null;
  return (
    <>
      <div className={cx('ts-overlay', contained && 'ts-overlay--contained')} style={{ background: 'rgba(22,24,29,.35)' }} onClick={onClose} />
      <aside className={cx('ts-drawer', contained && 'ts-drawer--contained')} style={{ width }} role="dialog" aria-modal="true">
        <div className="ts-drawer__head">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flex: 1 }}><span style={{ font: '600 18px var(--font-sans)' }}>{title}</span>{subtitle ? <span className="ts-block__sub">{subtitle}</span> : null}</div>
          {onClose ? <IconButton icon="x" label="Закрыть" size="sm" onClick={onClose} /> : null}
        </div>
        <div className="ts-drawer__body">{children}</div>
        {footer ? <div className="ts-drawer__foot">{footer}</div> : null}
      </aside>
    </>
  );
}
