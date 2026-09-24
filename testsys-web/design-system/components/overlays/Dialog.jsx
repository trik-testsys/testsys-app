import React from 'react';
import { IconButton } from '../actions/IconButton.jsx';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Dialog({ open = true, title, children, footer, onClose, variant = 'default', size = 'sm', contained = false }) {
  React.useEffect(() => {
    if (!open || !onClose) return;
    const h = e => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', h); return () => window.removeEventListener('keydown', h);
  }, [open, onClose]);
  if (!open) return null;
  const alert = variant === 'danger';
  return (
    <div className={cx('ts-overlay', contained && 'ts-overlay--contained')} onClick={onClose}>
      <div role="dialog" aria-modal="true" className={cx('ts-dialog', size === 'md' && 'ts-dialog--md', alert && 'ts-dialog--alert')} onClick={e => e.stopPropagation()}>
        {alert ? (
          <div className="ts-dialog__body">
            <span className="ts-dialog__glyph"><Icon name="triangle-alert" size={20} /></span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}><h2 className="ts-dialog__title" style={{ color: 'var(--text-primary)' }}>{title}</h2><div>{children}</div></div>
          </div>
        ) : (
          <>
            <div className="ts-dialog__head"><h2 className="ts-dialog__title">{title}</h2>{onClose ? <IconButton icon="x" label="Закрыть" size="sm" onClick={onClose} /> : null}</div>
            <div className="ts-dialog__body">{children}</div>
          </>
        )}
        {footer ? <div className="ts-dialog__foot">{footer}</div> : null}
      </div>
    </div>
  );
}
