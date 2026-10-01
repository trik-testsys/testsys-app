import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Page({ header, head, children, className, style }) {
  return (
    <div className={cx('ts-app', className)} style={style}>
      {header}
      {head}
      <main className="ts-page">{children}</main>
    </div>
  );
}
