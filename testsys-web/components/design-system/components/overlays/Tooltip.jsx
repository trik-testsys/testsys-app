import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Tooltip({ content, placement = 'top', open, children, static: isStatic = false }) {
  const id = React.useId();
  const tip = <span id={id} role="tooltip" className={cx('ts-tooltip', 'ts-tooltip--' + placement)}>{content}<span className="ts-tooltip__arrow" /></span>;
  if (isStatic) return tip;
  return <span className={cx('ts-tipwrap', open && 'ts-tipwrap--open')}>{React.isValidElement(children) ? React.cloneElement(children, { 'aria-describedby': [children.props['aria-describedby'], id].filter(Boolean).join(' ') }) : children}{tip}</span>;
}
