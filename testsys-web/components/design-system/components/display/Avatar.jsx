import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Avatar({ name = '', initials, size = 32, tone, square = false, className, style }) {
  const ini = initials || name.split(/\s+/).filter(Boolean).map(w => w[0]).join('').slice(0, 2).toUpperCase();
  const t = tone != null ? tone : [...name].reduce((a, c) => a + c.charCodeAt(0), 0) % 4;
  return (
    <span className={cx('ts-avatar', square && 'ts-avatar--square', t === 'dark' ? 'ts-avatar--dark' : 'ts-avatar--t' + t, className)}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.36), ...style }}>{ini}</span>
  );
}
