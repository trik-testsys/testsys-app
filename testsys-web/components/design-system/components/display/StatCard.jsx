import React from 'react';
import { Block } from '../layout/Block.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function StatCard({ label, value, delta, trend, dark = false, children, span, className }) {
  return (
    <Block dark={dark} span={span} className={className} bodyStyle={{ gap: 6 }}>
      <span className="ts-stat__label">{label}</span>
      <span className="ts-stat__value">{value}</span>
      {delta ? <span className={cx('ts-stat__delta', trend && 'ts-stat__delta--' + trend)}>{delta}</span> : null}
      {children}
    </Block>
  );
}
