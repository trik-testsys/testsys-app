import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Select({ options = [], value, onChange, placeholder = 'Выберите', error = false, disabled = false, className }) {
  const [open, setOpen] = React.useState(false);
  const ref = React.useRef(null);
  React.useEffect(() => {
    const h = e => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', h); return () => document.removeEventListener('mousedown', h);
  }, []);
  const opts = options.map(o => typeof o === 'string' ? { value: o, label: o } : o);
  const cur = opts.find(o => o.value === value);
  return (
    <div ref={ref} style={{ position: 'relative' }} className={className}>
      <button type="button" disabled={disabled} onClick={() => setOpen(!open)}
        className={cx('ts-trigger', open && 'ts-trigger--open', error && 'ts-trigger--error', disabled && 'ts-trigger--disabled')}>
        {cur ? <span>{cur.label}</span> : <span className="ts-trigger__placeholder">{placeholder}</span>}
        <span className="ts-trigger__chev"><Icon name="chevron-down" /></span>
      </button>
      {open ? (
        <div className="ts-popover ts-dropdown">
          <div className="ts-options">
            {opts.map(o => (
              <button type="button" key={o.value} className={cx('ts-option', o.value === value && 'ts-option--selected')} onClick={() => { onChange && onChange(o.value); setOpen(false); }}>
                <span>{o.label}</span>{o.meta ? <span className="ts-option__meta">{o.meta}</span> : null}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}
