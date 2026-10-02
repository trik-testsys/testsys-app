import React from 'react';
import { Icon } from '../core/Icon.jsx';
import { Checkbox } from './Checkbox.jsx';
import { Button } from '../actions/Button.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function MultiSelect({ options = [], value = [], onChange, placeholder = 'Выберите', label, display = 'chips', maxChips = 3, searchable = true, searchPlaceholder = 'Найти', showSelectAll = true, onApply, error = false, disabled = false, defaultOpen = false, className, id, ...rest }) {
  const [open, setOpen] = React.useState(defaultOpen);
  const [q, setQ] = React.useState('');
  const ref = React.useRef(null);
  React.useEffect(() => {
    const h = e => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', h); return () => document.removeEventListener('mousedown', h);
  }, []);
  const opts = options.map(o => typeof o === 'string' ? { value: o, label: o } : o);
  const vis = opts.filter(o => String(o.label).toLowerCase().includes(q.trim().toLowerCase()));
  const set = v => onChange && onChange(v);
  const toggle = v => set(value.includes(v) ? value.filter(x => x !== v) : [...value, v]);
  const allOn = vis.length > 0 && vis.every(o => value.includes(o.value));
  const some = !allOn && vis.some(o => value.includes(o.value));
  const selected = opts.filter(o => value.includes(o.value));
  const shown = selected.slice(0, maxChips);
  return (
    <div ref={ref} onKeyDown={e => { if (e.key === 'Escape' && open) { e.stopPropagation(); e.preventDefault(); setOpen(false); ref.current.querySelector('[role="button"]')?.focus(); } }} style={{ position: 'relative' }} className={className}>
      <div id={id} {...rest} aria-label={rest['aria-label'] || label || placeholder} aria-expanded={open} aria-disabled={disabled} onKeyDown={e => { if (e.target !== e.currentTarget) return; if (!disabled && ['Enter', ' ', 'ArrowDown'].includes(e.key)) { e.preventDefault(); setOpen(true); } if (e.key === 'Escape') setOpen(false); }} role="button" tabIndex={disabled ? -1 : 0} onClick={() => !disabled && setOpen(!open)}
        className={cx('ts-trigger', open && 'ts-trigger--open', error && 'ts-trigger--error', disabled && 'ts-trigger--disabled')} style={{ paddingLeft: selected.length && display === 'chips' ? 6 : 12 }}>
        {selected.length === 0 ? <span className="ts-trigger__placeholder">{placeholder}</span> : display === 'count' ? (
          <><span>{label || placeholder}</span><span className="ts-counter ts-counter--accent">{selected.length}</span></>
        ) : (
          <>
            {shown.map(o => (
              <span key={o.value} className="ts-chip">{o.label}
                {!disabled ? <button type="button" className="ts-chip__x" aria-label="Убрать" onClick={e => { e.stopPropagation(); toggle(o.value); }}><Icon name="x" size={12} strokeWidth={2.5} /></button> : null}
              </span>
            ))}
            {selected.length > maxChips ? <span className="ts-chip ts-chip--more">+{selected.length - maxChips}</span> : null}
          </>
        )}
        <span style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 6 }}>
          {selected.length && !disabled ? <button type="button" className="ts-chip__x" style={{ width: 24, height: 24, color: 'var(--text-secondary)' }} aria-label="Очистить" onClick={e => { e.stopPropagation(); set([]); }}><Icon name="x" size={14} /></button> : null}
          <span className="ts-trigger__chev" style={{ marginLeft: 0 }}><Icon name="chevron-down" /></span>
        </span>
      </div>
      {open ? (
        <div className="ts-popover ts-dropdown">
          {searchable ? <div className="ts-popover__search"><div><Icon name="search" /><input aria-label={searchPlaceholder} value={q} onChange={e => setQ(e.target.value)} placeholder={searchPlaceholder} autoFocus /></div></div> : null}
          <div className="ts-options">
            {showSelectAll && vis.length > 1 ? (
              <button type="button" className="ts-option" style={{ fontWeight: 600 }} onClick={() => set(allOn ? value.filter(v => !vis.some(o => o.value === v)) : [...new Set([...value, ...vis.map(o => o.value)])])}>
                <Checkbox checked={allOn} indeterminate={some} /> Выбрать все
              </button>
            ) : null}
            {vis.map(o => (
              <button type="button" aria-pressed={value.includes(o.value)} key={o.value} className="ts-option" onClick={() => toggle(o.value)}>
                <Checkbox checked={value.includes(o.value)} /><span>{o.label}</span>{o.meta != null ? <span className="ts-option__meta">{o.meta}</span> : null}
              </button>
            ))}
            {vis.length === 0 ? <div style={{ padding: '16px 10px', textAlign: 'center', color: 'var(--text-secondary)' }}>Ничего не найдено</div> : null}
          </div>
          <div className="ts-popover__foot">
            <span className="ts-muted" style={{ flex: 1, fontSize: 13 }}>Выбрано: {value.length}</span>
            <Button variant="ghost" size="sm" onClick={() => set([])}>Сбросить</Button>
            <Button variant="primary" size="sm" onClick={() => { setOpen(false); onApply && onApply(value); }}>Применить</Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
