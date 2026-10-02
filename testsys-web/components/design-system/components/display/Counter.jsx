import React from 'react';

export function Counter({ tone = 'danger', children }) {
  return <span className={'ts-counter ts-counter--' + tone}>{children}</span>;
}
