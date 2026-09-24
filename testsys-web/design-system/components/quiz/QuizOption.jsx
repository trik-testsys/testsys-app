import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function QuizOption({ letter, label, multiple = false, selected = false, result, onClick }) {
  const showMark = result === 'correct' || selected;
  return (
    <button type="button" onClick={result ? undefined : onClick}
      className={cx('ts-qopt', multiple && 'ts-qopt--multi', !result && selected && 'ts-qopt--selected', result === 'correct' && 'ts-qopt--correct', result === 'wrong' && 'ts-qopt--wrong', result && 'ts-qopt--locked')}>
      <span className="ts-qopt__ind">{showMark ? <Icon name="check" size={12} strokeWidth={3.5} /> : null}</span>
      {letter ? <span className="ts-qopt__letter">{letter}</span> : null}
      <span className="ts-qopt__label">{label}</span>
    </button>
  );
}
