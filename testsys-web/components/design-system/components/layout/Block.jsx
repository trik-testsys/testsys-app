import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Block({ span, grid = false, title, subtitle, actions, filters, footer, dark = false, sunken = false, flush = false, bodyStyle, children, className, style }) {
  const hasHead = title || subtitle || actions;
  return (
    <section className={cx('ts-block', grid && 'ts-block--grid', filters && 'ts-block--filters', dark && 'ts-block--dark', sunken && 'ts-block--sunken', className)} style={{ gridColumn: span ? 'span ' + span : undefined, ...style }}>
      {hasHead ? (
        <header className="ts-block__head">
          <div className="ts-block__titles">
            {title ? <h3 className="ts-block__title">{title}</h3> : null}
            {subtitle ? <span className="ts-block__sub">{subtitle}</span> : null}
          </div>
          {actions ? <div className="ts-block__actions">{actions}</div> : null}
        </header>
      ) : null}
      {filters}
      {children != null ? <div className={cx('ts-block__body', grid && 'ts-block__body--grid', flush && 'ts-block__body--flush')} style={bodyStyle}>{children}</div> : null}
      {footer ? <footer className="ts-block__foot">{footer}</footer> : null}
    </section>
  );
}

export function BlockRow({ children, className, style }) {
  return <div className={cx('ts-block__row', className)} style={style}>{children}</div>;
}
