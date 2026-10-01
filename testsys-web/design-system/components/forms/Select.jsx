import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Select({ options = [], value, onChange, placeholder = 'Выберите', error = false, disabled = false, className, id, ...rest }) {
  const [open, setOpen] = React.useState(false);
  const trigger = React.useRef(null);
  const listId = React.useId();
  const ref = React.useRef(null);
  React.useEffect(() => {
    const h = e => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', h); return () => document.removeEventListener('mousedown', h);
  }, []);
  const choose = value => { onChange?.(value); setOpen(false); trigger.current?.focus(); };
  const opts = options.map(o => typeof o === 'string' ? { value: o, label: o } : o);
  const cur = opts.find(o => o.value === value);
  return (
    <div ref={ref} onKeyDown={e => {
      if (e.key === 'Escape' && open) { e.stopPropagation(); e.preventDefault(); setOpen(false); trigger.current?.focus(); }
      if (['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(e.key)) {
        e.preventDefault(); if (!open) { setOpen(true); return; }
        const entries = Array.from(ref.current.querySelectorAll('[role="option"]')); const index = entries.indexOf(document.activeElement);
        const next = e.key === 'Home' ? 0 : e.key === 'End' ? entries.length - 1 : (index + (e.key === 'ArrowDown' ? 1 : -1) + entries.length) % entries.length; entries[next]?.focus();
      }
    }} style={{ position: 'relative' }} className={className}>
      <button ref={trigger} id={id} {...rest} aria-label={rest['aria-label'] || placeholder} aria-haspopup="listbox" aria-expanded={open} aria-controls={listId} type="button" disabled={disabled} onClick={() => setOpen(!open)}
        className={cx('ts-trigger', open && 'ts-trigger--open', error && 'ts-trigger--error', disabled && 'ts-trigger--disabled')}>
        {cur ? <span>{cur.label}</span> : <span className="ts-trigger__placeholder">{placeholder}</span>}
        <span className="ts-trigger__chev"><Icon name="chevron-down" /></span>
      </button>
      {open ? (
        <div className="ts-popover ts-dropdown">
          <div id={listId} role="listbox" className="ts-options">
            {opts.length === 0 ? <span role="status" className="ts-hint" style={{ padding: 8 }}>Нет доступных вариантов</span> : null}
            {opts.map(o => (
              <button type="button" role="option" aria-selected={o.value === value} key={o.value} className={cx('ts-option', o.value === value && 'ts-option--selected')} onClick={() => choose(o.value)}>
                <span>{o.label}</span>{o.meta ? <span className="ts-option__meta">{o.meta}</span> : null}
              </button>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}
