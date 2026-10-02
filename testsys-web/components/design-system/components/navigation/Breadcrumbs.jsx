import React from 'react';

export function Breadcrumbs({ items = [] }) {
  return (
    <nav className="ts-crumbs" aria-label="Навигация">
      {items.map((it, i) => (
        <React.Fragment key={i}>
          {i ? <span className="ts-crumbs__sep">/</span> : null}
          {i < items.length - 1 ? <a href={it.href || '#'} onClick={it.onClick}>{it.label}</a> : <span>{it.label}</span>}
        </React.Fragment>
      ))}
    </nav>
  );
}
