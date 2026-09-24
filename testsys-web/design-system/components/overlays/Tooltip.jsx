import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Tooltip({ content, placement = 'top', open, children, static: isStatic = false }) {
  const tip = <span role="tooltip" className={cx('ts-tooltip', 'ts-tooltip--' + placement)}>{content}<span className="ts-tooltip__arrow" /></span>;
  if (isStatic) return tip;
  return <span className={cx('ts-tipwrap', open && 'ts-tipwrap--open')}>{children}{tip}</span>;
}
