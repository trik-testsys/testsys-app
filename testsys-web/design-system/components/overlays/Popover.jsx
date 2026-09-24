import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Popover({ trigger, open: openProp, defaultOpen = false, onOpenChange, align = 'left', width, children }) {
  const [inner, setInner] = React.useState(defaultOpen);
  const open = openProp != null ? openProp : inner;
  const set = v => { setInner(v); onOpenChange && onOpenChange(v); };
  const ref = React.useRef(null);
  React.useEffect(() => {
    const h = e => { if (ref.current && !ref.current.contains(e.target)) set(false); };
    document.addEventListener('mousedown', h); return () => document.removeEventListener('mousedown', h);
  }, []);
  return (
    <span ref={ref} style={{ position: 'relative', display: 'inline-flex', alignSelf: 'flex-start', height: 'fit-content' }}>
      <span onClick={() => set(!open)} style={{ display: 'inline-flex' }}>{trigger}</span>
      {open ? <div className="ts-popover ts-pop" style={{ position: 'absolute', top: 'calc(100% + 6px)', [align === 'right' ? 'right' : 'left']: 0, zIndex: 40, width }}>{children}</div> : null}
    </span>
  );
}
