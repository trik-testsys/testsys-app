import React from 'react';
import { Icon } from './client-icons.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function SortableList({ items, getKey, onChange, renderItem, renderDragItem, handleLabel, announcement, disabled }) {
  const [drag, setDrag] = React.useState(null);
  const [dropped, setDropped] = React.useState(null);
  const refs = React.useRef(Object.create(null));
  const rects = React.useRef([]);
  const keys = items.map(getKey);
  const timer = React.useRef();
  const dragRef = React.useRef();
  dragRef.current = drag;
  React.useEffect(() => { setDrag(null); }, [items, disabled]);
  React.useEffect(() => () => clearTimeout(timer.current), []);
  const commit = d => {
    if (!d) return;
    const rest = items.filter((_, i) => keys[i] !== d.key);
    rest.splice(d.index, 0, items[keys.indexOf(d.key)]);
    if (d.index !== d.from && onChange) onChange(rest);
    setDropped(d.key); clearTimeout(timer.current); timer.current = setTimeout(() => setDropped(null), 1200);
    setDrag(null);
  };
  React.useEffect(() => {
    if (!drag || drag.mode !== 'pointer') return;
    const move = e => setDrag(d => {
      if (!d) return null;
      const center = e.clientY - d.offset + d.height / 2;
      let index = 0;
      rects.current.forEach(r => { if (r.key !== d.key && center > r.top + r.height / 2) index++; });
      return {...d, y: e.clientY, index};
    });
    const up = () => commit(dragRef.current);
    const cancel = () => setDrag(null);
    const escape = e => { if (e.key === 'Escape') cancel(); };
    window.addEventListener('pointermove', move); window.addEventListener('pointerup', up);
    window.addEventListener('pointercancel', cancel); window.addEventListener('keydown', escape);
    document.body.classList.add('ts-dragging');
    return () => { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', up); window.removeEventListener('pointercancel', cancel); window.removeEventListener('keydown', escape); document.body.classList.remove('ts-dragging'); };
  }, [drag && drag.key, drag && drag.mode, items]);
  const start = (e, key, i) => {
    if (disabled || e.button !== 0) return;
    e.preventDefault();
    rects.current = keys.map(k => { const r = refs.current[k].getBoundingClientRect(); return {key:k, top:r.top, height:r.height}; });
    const r = refs.current[key].getBoundingClientRect();
    setDrag({key, from:i, index:i, y:e.clientY, offset:e.clientY-r.top, height:r.height, width:r.width, left:r.left, mode:'pointer'});
  };
  const keyboard = (e, key, i) => {
    if (disabled) return;
    if (e.key === 'Escape') { e.preventDefault(); setDrag(null); return; }
    if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); if (drag?.mode === 'keyboard') commit(drag); else setDrag({key, from:i, index:i, mode:'keyboard'}); }
    if (drag?.key === key && drag.mode === 'keyboard' && ['ArrowUp','ArrowDown','Home','End'].includes(e.key)) {
      e.preventDefault(); setDrag({...drag, index:e.key === 'Home' ? 0 : e.key === 'End' ? items.length-1 : Math.max(0, Math.min(items.length-1, drag.index + (e.key === 'ArrowUp' ? -1 : 1)))});
    }
  };
  let view = items.map((it, i) => ({it, key:keys[i]}));
  if (drag) { const moved = view.splice(keys.indexOf(drag.key), 1)[0]; view.splice(drag.index, 0, moved); }
  const row = (it, i, key) => <div className={cx('ts-sort-item', dropped === key && 'ts-sort-item--dropped')}>
    <button type="button" className="ts-sort-handle" disabled={disabled} aria-label={handleLabel} aria-pressed={drag?.key === key} onPointerDown={e => start(e, key, keys.indexOf(key))} onKeyDown={e => keyboard(e, key, keys.indexOf(key))}><Icon name="grip-vertical" size={16} /></button>
    <div className="ts-sort-content">{renderItem(it, i)}</div>
  </div>;
  const live = drag ? (announcement(items[keys.indexOf(drag.key)], drag.index + 1, items.length)) : '';
  return <div className="ts-sortable" style={{gap: 'var(--space-2)'}}>
    <span className="ts-sr-only" role="status" aria-live="polite">{live}</span>
    {view.map((v, i) => <div key={v.key} ref={el => { if (el) refs.current[v.key] = el; else delete refs.current[v.key]; }} className={drag?.mode === 'pointer' && drag.key === v.key ? 'ts-sort-slot' : undefined} style={drag?.mode === 'pointer' && drag.key === v.key ? {height:drag.height} : undefined}>
      <div style={drag?.mode === 'pointer' && drag.key === v.key ? {visibility:'hidden'} : undefined}>{row(v.it, i, v.key)}</div>
    </div>)}
    {drag?.mode === 'pointer' ? <div className="ts-sort-ghost" aria-hidden="true" style={{top:drag.y-drag.offset,left:drag.left,width:drag.width}}><div className="ts-sort-item"><span className="ts-sort-handle"><Icon name="grip-vertical" size={16} /></span><div className="ts-sort-content">{renderDragItem(items[keys.indexOf(drag.key)])}</div></div></div> : null}
  </div>;
}
