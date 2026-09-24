import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Row({ align = 'start', tight = false, children, className, style }) {
  return <div className={cx('ts-row', align === 'stretch' && 'ts-row--stretch', tight && 'ts-row--tight', className)} style={style}>{children}</div>;
}
export function Stack({ span, children, className, style }) {
  return <div className={cx('ts-stack', className)} style={{ gridColumn: span ? 'span ' + span : undefined, ...style }}>{children}</div>;
}
