import React from 'react';

// The server and client renderers share one Lucide sprite.
export function Icon({name, size = 16}) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{flex: 'none'}}>
    <use href={'testsys-ui/icons.svg#' + name} />
  </svg>;
}

// Internal calendar control, not a second public actions API.
export function IconButton({icon, label, variant, size, ...rest}) {
  return <button type="button" aria-label={label} title={label}
    className={'ts-btn ts-btn--icon ts-btn--' + variant + ' ts-btn--' + size} {...rest}>
    <Icon name={icon} />
  </button>;
}
