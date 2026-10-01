import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function QuestionNav({ total = 10, current = 1, answered = [], flagged = [], onSelect }) {
  return (
    <div className="ts-qnav">
      {Array.from({ length: total }, (_, i) => {
        const n = i + 1;
        return (
          <button type="button" key={n} onClick={() => onSelect && onSelect(n)} className={cx('ts-qnav__cell', n === current ? 'ts-qnav__cell--current' : answered.includes(n) && 'ts-qnav__cell--answered')}>
            {n}{flagged.includes(n) ? <span className="ts-qnav__flag" /> : null}
          </button>
        );
      })}
    </div>
  );
}
