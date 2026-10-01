import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

const LABELS = { ok: 'Принято', wa: 'Неверный ответ', tle: 'Превышено время', mle: 'Превышена память', re: 'Ошибка выполнения', ce: 'Ошибка компиляции', queue: 'В очереди' };
const CODES = { ok: 'OK', wa: 'WA', tle: 'TLE', mle: 'MLE', re: 'RE', ce: 'CE', queue: '…' };
export function Verdict({ score, label, code = 'ok', showLabel = false, children, className }) {
  if (score != null) return <span className={cx('ts-verdict', 'ts-verdict--score', className)}>{score}{label ? <span className="ts-verdict__label">{label}</span> : null}</span>;
  return <span className={cx('ts-verdict', 'ts-verdict--' + code, className)}>{children ?? CODES[code]}{showLabel ? <span className="ts-verdict__label">{LABELS[code]}</span> : null}</span>;
}
