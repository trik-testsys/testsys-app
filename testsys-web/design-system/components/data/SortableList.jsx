import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function SortableList({ items = [], getKey = (it, i) => (it && it.id != null ? it.id : i), onChange, renderItem, gap = 8, disabled = false, className }) {
  const [drag, setDrag] = React.useState(null);
  const [dropped, setDropped] = React.useState(null);
  const refs = React.useRef({});
  const rects = React.useRef([]);
  const keys = items.map(getKey);

  const targetIndex = (d, y) => {
    const center = y - d.offset + d.height / 2;
    let idx = 0;
    rects.current.forEach(r => { if (r.key !== d.key && center > r.top + r.height / 2) idx++; });
    return idx;
  };

  React.useEffect(() => {
    if (!drag) return;
    const move = e => setDrag(d => d && { ...d, y: e.clientY, index: targetIndex(d, e.clientY) });
    const up = () => setDrag(d => {
      if (d) {
        const rest = items.filter((_, i) => keys[i] !== d.key);
        const moved = items[keys.indexOf(d.key)];
        rest.splice(d.index, 0, moved);
        if (d.index !== d.from && onChange) onChange(rest);
        setDropped(d.key); setTimeout(() => setDropped(null), 1200);
      }
      return null;
    });
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up, { once: true });
    document.body.classList.add('ts-dragging');
    return () => { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', up); document.body.classList.remove('ts-dragging'); };
  }, [drag && drag.key]);

  const start = (e, key, i) => {
    if (disabled || e.button !== 0) return;
    e.preventDefault();
    rects.current = keys.map(k => { const r = refs.current[k].getBoundingClientRect(); return { key: k, top: r.top, height: r.height }; });
    const r = refs.current[key].getBoundingClientRect();
    setDrag({ key, from: i, index: i, y: e.clientY, offset: e.clientY - r.top, height: r.height, width: r.width, left: r.left });
  };

  const row = (it, i, extra) => (
    <div className={cx('ts-sort-item', extra)} >
      <span className="ts-sort-handle" onPointerDown={e => start(e, getKey(it, i), i)} aria-label="Перетащить"><Icon name="grip-vertical" size={16} /></span>
      <div className="ts-sort-content">{renderItem ? renderItem(it, i) : String(it)}</div>
    </div>
  );

  let view = items.map((it, i) => ({ it, key: keys[i] }));
  let draggedItem = null;
  if (drag) {
    draggedItem = items[keys.indexOf(drag.key)];
    view = view.filter(v => v.key !== drag.key);
    view.splice(drag.index, 0, { slot: true, key: '__slot' });
  }
  let pos = 0;
  return (
    <div className={cx('ts-sortable', className)} style={{ gap }}>
      {view.map(v => {
        if (v.slot) { pos++; return <div key="__slot" className="ts-sort-slot" style={{ height: drag.height }} />; }
        const i = pos++;
        return <div key={v.key} ref={el => { if (el) refs.current[v.key] = el; }}>{row(v.it, i, dropped === v.key && 'ts-sort-item--dropped')}</div>;
      })}
      {drag ? (
        <div className="ts-sort-ghost" style={{ top: drag.y - drag.offset, left: drag.left, width: drag.width }}>
          {row(draggedItem, drag.index)}
        </div>
      ) : null}
    </div>
  );
}
