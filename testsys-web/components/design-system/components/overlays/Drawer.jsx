import React from 'react';
import { IconButton } from '../actions/IconButton.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Drawer({ open = true, title, subtitle, children, footer, onClose, width = 480, contained = false }) {
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
  return (
    <>
      <div className={cx('ts-overlay', contained && 'ts-overlay--contained')} style={{ background: 'rgba(22,24,29,.35)' }} onClick={onClose} />
      <aside ref={panel} onKeyDown={handleKeyDown} tabIndex={-1} aria-labelledby={titleId} className={cx('ts-drawer', contained && 'ts-drawer--contained')} style={{ width }} role="dialog" aria-modal="true">
        <div className="ts-drawer__head">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flex: 1 }}><span id={titleId} style={{ font: '600 18px var(--font-sans)' }}>{title}</span>{subtitle ? <span className="ts-block__sub">{subtitle}</span> : null}</div>
          {onClose ? <IconButton icon="x" label="Закрыть" size="sm" onClick={onClose} /> : null}
        </div>
        <div className="ts-drawer__body">{children}</div>
        {footer ? <div className="ts-drawer__foot">{footer}</div> : null}
      </aside>
    </>
  );
}
