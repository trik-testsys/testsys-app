import React from 'react';
import { Popover } from './Popover.jsx';
import { IconButton } from '../actions/IconButton.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Menu({ items = [], onSelect, trigger, defaultOpen = false, align = 'right' }) {
  const [open, setOpen] = React.useState(defaultOpen);
  const root = React.useRef(null);
  const close = () => { setOpen(false); root.current?.querySelector('[aria-expanded]')?.focus(); };
  return (
    <span ref={root} style={{ display: 'inline-flex' }}><Popover open={open} onOpenChange={setOpen} align={align} trigger={trigger || <IconButton icon="ellipsis" label="Действия" size="sm" />}>
      <div className="ts-menu" role="menu" onKeyDown={e => {
        if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(e.key)) return;
        e.preventDefault(); const entries = Array.from(e.currentTarget.querySelectorAll('button:not([disabled])')); const index = entries.indexOf(document.activeElement);
        const next = e.key === 'Home' ? 0 : e.key === 'End' ? entries.length - 1 : (index + (e.key === 'ArrowDown' ? 1 : -1) + entries.length) % entries.length;
        entries[next]?.focus();
      }}>
        {items.map((it, i) => it.separator ? <div key={i} className="ts-menu__sep" /> : (
          <button type="button" disabled={it.disabled} role="menuitem" key={i} className={cx('ts-menu__item', it.danger && 'ts-menu__item--danger')} onClick={() => { close(); it.onClick?.(); onSelect && onSelect(it.value || it.label); }}>
            <span>{it.label}</span>
          </button>
        ))}
      </div>
    </Popover></span>
  );
}
