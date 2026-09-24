import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Stepper({ steps = [], current = 0, onStepClick }) {
  return (
    <div className="ts-stepper">
      {steps.map((s, i) => {
        const done = i < current, cur = i === current, last = i === steps.length - 1;
        return (
          <button type="button" key={i} className={cx('ts-step', !last && 'ts-step--grow', done && 'ts-step--done', cur && 'ts-step--current')} onClick={() => onStepClick && onStepClick(i)}>
            <span className="ts-step__dot">{done ? <Icon name="check" size={14} strokeWidth={3} /> : i + 1}</span>
            <span className="ts-step__text"><span className="ts-step__label">{s.label}</span>{s.sub ? <span className="ts-step__sub">{s.sub}</span> : null}</span>
            {!last ? <span className="ts-step__line" /> : null}
          </button>
        );
      })}
    </div>
  );
}
