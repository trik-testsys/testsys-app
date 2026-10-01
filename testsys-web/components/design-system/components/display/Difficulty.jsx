import React from 'react';

const C = { 1: 'var(--success)', 2: 'var(--warning)', 3: 'var(--danger)' };
const L = { 1: 'Лёгкая', 2: 'Средняя', 3: 'Сложная' };
export function Difficulty({ level = 1, label }) {
  return (
    <span className="ts-difficulty">
      <span>{[1, 2, 3].map(i => <i key={i} style={{ background: i <= level ? C[level] : undefined }} />)}</span>
      {label === false ? null : label || L[level]}
    </span>
  );
}
