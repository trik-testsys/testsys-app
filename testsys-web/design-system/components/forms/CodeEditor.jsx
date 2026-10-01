import React from 'react';

export function CodeEditor({ value = '', onChange, minHeight = 240, readOnly = false, label = 'Редактор кода', id, ...rest }) {
  const lines = Math.max(1, value.split('\n').length);
  return (
    <div className="ts-code" style={{ minHeight }}>
      <div className="ts-code__lines">{Array.from({ length: lines }, (_, i) => i + 1).join('\n')}</div>
      <textarea id={id} aria-label={label} {...rest} className="ts-code__area" spellCheck={false} readOnly={readOnly} value={value} onChange={e => onChange && onChange(e.target.value)} style={{ minHeight }} />
    </div>
  );
}
