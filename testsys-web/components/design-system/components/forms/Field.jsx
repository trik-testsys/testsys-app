import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Field({ label, hint, error, aside, disabled = false, labelSize, size, controlId, required = false, children, className, style }) {
  const generated = React.useId();
  const id = controlId || 'field-' + generated;
  if ((labelSize == null) !== (size == null) || (labelSize != null && (!Number.isInteger(labelSize) || labelSize < 1 || !Number.isInteger(size) || size < 1))) throw new Error('Field labelSize and size must both be positive integer columns');
  const grid = labelSize != null && size != null;
  const descriptionId = id + '-description';
  const control = React.isValidElement(children) ? React.cloneElement(children, {
    id: children.props.id || id,
    'aria-label': children.props['aria-label'] || (typeof label === 'string' ? label : undefined),
    'aria-describedby': cx(children.props['aria-describedby'], (hint || error) && descriptionId) || undefined,
    'aria-invalid': error ? true : children.props['aria-invalid'],
    'aria-required': required || undefined,
    disabled: disabled || children.props.disabled,
  }) : children;
  const caption = label ? <label onClick={() => {
    const target = document.getElementById(React.isValidElement(children) ? children.props.id || id : id);
    if (target?.matches('button, input, textarea, select')) return;
    const focusable = target?.matches('[tabindex], [role="button"]') ? target : target?.querySelector('button, input, textarea, select, [tabindex]');
    focusable?.focus();
  }} htmlFor={React.isValidElement(children) ? children.props.id || id : undefined} className={cx(grid ? 'ts-field__label' : 'ts-label', disabled && 'ts-label--disabled')} style={grid ? { gridColumn: 'span ' + labelSize } : undefined}><span>{label}{required ? <span className="ts-field__required" aria-hidden="true">*</span> : null}</span>{aside}</label> : null;
  const description = error || hint ? <span id={descriptionId} className={error ? 'ts-error' : 'ts-hint'}>{error || hint}</span> : null;
  return <div className={cx('ts-field', grid && 'ts-field--grid', disabled && 'ts-field--disabled', className)} style={{ ...(grid ? { gridColumn: 'span ' + (labelSize + size) } : {}), ...style }}>
    {caption}{grid ? <div className="ts-field__value ts-vstack" style={{ gridColumn: 'span ' + size }}>{control}{description}</div> : <>{control}{description}</>}
  </div>;
}
