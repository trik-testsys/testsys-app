import React from 'react';
import { Icon } from '../core/Icon.jsx';
const ICON = { success: 'check', error: 'x', info: 'info', warning: 'triangle-alert' };
export function Toast({ tone = 'info', title, description, onClose }) {
  return (
    <div className={'ts-toast ts-toast--' + tone} role="status">
      <span className="ts-toast__icon"><Icon name={ICON[tone]} size={14} strokeWidth={2.6} /></span>
      <div className="ts-toast__text"><span className="ts-toast__title">{title}</span>{description ? <span className="ts-toast__desc">{description}</span> : null}</div>
      {onClose ? <button type="button" className="ts-toast__close" aria-label="Закрыть" onClick={onClose}><Icon name="x" /></button> : null}
    </div>
  );
}
export function useToasts(timeout = 4000) {
  const [list, setList] = React.useState([]);
  const push = React.useCallback((t) => {
    const id = Math.random().toString(36).slice(2);
    setList(l => [...l, { id, ...t }]);
    if (timeout) setTimeout(() => setList(l => l.filter(x => x.id !== id)), timeout);
  }, [timeout]);
  const remove = id => setList(l => l.filter(x => x.id !== id));
  const node = <div className="ts-toaster">{list.map(t => <Toast key={t.id} {...t} onClose={() => remove(t.id)} />)}</div>;
  return [push, node];
}
