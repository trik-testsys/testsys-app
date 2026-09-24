import React from 'react';
import { Popover } from './Popover.jsx';
import { IconButton } from '../actions/IconButton.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Menu({ items = [], onSelect, trigger, defaultOpen = false, align = 'right' }) {
  const [open, setOpen] = React.useState(defaultOpen);
  return (
    <Popover open={open} onOpenChange={setOpen} align={align} trigger={trigger || <IconButton icon="ellipsis" label="Действия" size="sm" />}>
      <div className="ts-menu" role="menu">
        {items.map((it, i) => it.separator ? <div key={i} className="ts-menu__sep" /> : (
          <button type="button" role="menuitem" key={i} className={cx('ts-menu__item', it.danger && 'ts-menu__item--danger')} onClick={() => { setOpen(false); onSelect && onSelect(it.value || it.label); }}>
            <span>{it.label}</span>{it.kbd ? <span className="ts-menu__kbd">{it.kbd}</span> : null}
          </button>
        ))}
      </div>
    </Popover>
  );
}
