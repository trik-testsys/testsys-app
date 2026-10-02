import React from 'react';
import { IconButton } from '../actions/IconButton.jsx';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Dialog({ open = true, title, children, footer, onClose, variant = 'default', size = 'sm', contained = false }) {
  const panel = React.useRef(null);
  const titleId = React.useId();
  const focusables = root => Array.from(root.querySelectorAll('button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), a[href], [tabindex="0"]')).filter(el => el.getClientRects().length);
  const handleKeyDown = e => {
    const root = panel.current;
    if (e.defaultPrevented || !root || !root.contains(document.activeElement)) return;
    if (e.key === 'Escape') { e.stopPropagation(); e.preventDefault(); onClose?.(); }
    if (e.key === 'Tab') {
      e.stopPropagation();
      const list = focusables(root); const first = list[0] || root; const last = list[list.length - 1] || root;
      if (!list.length || (e.shiftKey && document.activeElement === first) || (!e.shiftKey && document.activeElement === last)) { e.preventDefault(); (e.shiftKey ? last : first).focus(); }
    }
  };
  React.useEffect(() => {
    if (!open || !panel.current) return;
    const previous = document.activeElement;
    const root = panel.current;
    (focusables(root)[0] || root).focus();
    return () => { if (previous && previous.isConnected) previous.focus(); };
  }, [open]);
  if (!open) return null;
  const alert = variant === 'danger';
  return (
    <div className={cx('ts-overlay', contained && 'ts-overlay--contained')} onClick={onClose}>
      <div ref={panel} onKeyDown={handleKeyDown} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby={titleId} className={cx('ts-dialog', size === 'md' && 'ts-dialog--md', alert && 'ts-dialog--alert')} onClick={e => e.stopPropagation()}>
        {alert ? (
          <div className="ts-dialog__body">
            <span className="ts-dialog__glyph"><Icon name="triangle-alert" size={20} /></span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}><h2 id={titleId} className="ts-dialog__title" style={{ color: 'var(--text-primary)' }}>{title}</h2><div>{children}</div></div>
          </div>
        ) : (
          <>
            <div className="ts-dialog__head"><h2 id={titleId} className="ts-dialog__title">{title}</h2>{onClose ? <IconButton icon="x" label="Закрыть" size="sm" onClick={onClose} /> : null}</div>
            <div className="ts-dialog__body">{children}</div>
          </>
        )}
        {footer ? <div className="ts-dialog__foot">{footer}</div> : null}
      </div>
    </div>
  );
}
