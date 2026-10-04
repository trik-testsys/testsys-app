import React from 'react';
import { Footer } from './Footer.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Page({ header, head, footer = <Footer />, children, className, style }) {
  return (
    <div className={cx('ts-app', className)} style={style}>
      {header}
      {head}
      <main className="ts-page">{children}</main>
      {footer}
    </div>
  );
}
