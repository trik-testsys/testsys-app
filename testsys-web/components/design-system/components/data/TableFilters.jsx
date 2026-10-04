import React from 'react';
import { Button } from '../actions/Button.jsx';
import { Icon } from '../core/Icon.jsx';

export function TableFilters({ title = 'Фильтры', applyLabel = 'Применить', resetLabel = 'Сбросить', columns = 24, onApply, onReset, onRefresh, children }) {
  const [expanded, setExpanded] = React.useState(false);
  const id = React.useId();
  const content = React.useRef(null);
  const toggle = React.useRef(null);
  const collapse = () => {
    if (content.current?.contains(document.activeElement)) toggle.current?.focus();
    setExpanded(false);
  };
  return <div className="ts-table-filters">
    <button ref={toggle} id={id + '-toggle'} type="button" className="ts-table-filters__toggle" aria-expanded={expanded} aria-controls={id} onClick={() => expanded ? collapse() : setExpanded(true)}><Icon name="chevron-down" />{title}</button>
    <div ref={content} id={id} className="ts-table-filters__content" role="region" aria-labelledby={id + '-toggle'} hidden={!expanded}>
      <div className="ts-table-filters__fields" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>{children}</div>
      <div className="ts-table-filters__actions">
        <Button variant="secondary" onClick={() => { onReset(); onRefresh(); }}>{resetLabel}</Button>
        <Button onClick={() => { if (onApply()) onRefresh(); }}>{applyLabel}</Button>
      </div>
    </div>
  </div>;
}
