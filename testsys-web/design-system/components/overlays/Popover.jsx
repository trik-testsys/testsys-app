import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Popover({ trigger, open: openProp, defaultOpen = false, onOpenChange, align = 'left', width, children }) {
  const [inner, setInner] = React.useState(defaultOpen);
  const open = openProp != null ? openProp : inner;
  const set = v => { setInner(v); onOpenChange && onOpenChange(v); };
  const ref = React.useRef(null);
  const triggerRef = React.useRef(null);
  const panelId = React.useId();
  React.useEffect(() => {
    const h = e => { if (ref.current && !ref.current.contains(e.target)) set(false); };
    document.addEventListener('mousedown', h); return () => document.removeEventListener('mousedown', h);
  }, [onOpenChange]);
  React.useEffect(() => {
    if (open) ref.current?.querySelector('.ts-pop button:not([disabled]), .ts-pop input, .ts-pop [tabindex="0"]')?.focus();
  }, [open]);
  const close = () => { set(false); triggerRef.current?.querySelector('button, a, [tabindex]')?.focus(); };
  return (
    <span ref={ref} onKeyDown={e => { if (e.key === 'Escape' && open) { e.stopPropagation(); close(); } }} style={{ position: 'relative', display: 'inline-flex', alignSelf: 'flex-start', height: 'fit-content' }}>
      <span ref={triggerRef} style={{ display: 'inline-flex' }}>{React.isValidElement(trigger) ? React.cloneElement(trigger, { 'aria-expanded': open, 'aria-controls': panelId, onClick: e => { trigger.props.onClick?.(e); set(!open); } }) : <button type="button" aria-expanded={open} onClick={() => set(!open)}>{trigger}</button>}</span>
      {open ? <div id={panelId} className="ts-popover ts-pop" style={{ position: 'absolute', top: 'calc(100% + 6px)', [align === 'right' ? 'right' : 'left']: 0, zIndex: 40, width }}>{children}</div> : null}
    </span>
  );
}
