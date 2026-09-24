import React from 'react';
import { IconButton } from '../actions/IconButton.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
const WD = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
const iso = (y, m, d) => y + '-' + String(m + 1).padStart(2, '0') + '-' + String(d).padStart(2, '0');

export function DateRangeCalendar({ year, month, start, end, today, range = true, onChange, onPrev, onNext, className }) {
  const offset = (new Date(year, month, 1).getDay() + 6) % 7;
  const count = new Date(year, month + 1, 0).getDate();
  const pick = d => {
    if (!onChange) return;
    if (!range) return onChange({ start: d, end: null });
    if (!start || end) return onChange({ start: d, end: null });
    if (d < start) return onChange({ start: d, end: null });
    onChange({ start, end: d });
  };
  const cells = [];
  for (let i = 0; i < offset; i++) cells.push(<span key={'b' + i} className="ts-cal__blank" />);
  for (let d = 1; d <= count; d++) {
    const k = iso(year, month, d), col = (offset + d - 1) % 7;
    const isS = k === start, isE = k === end, inR = start && end && k > start && k < end;
    cells.push(
      <button type="button" key={k} onClick={() => pick(k)} className={cx('ts-cal__day', col >= 5 && 'ts-cal__day--weekend', k === today && !isS && !isE && !inR && 'ts-cal__day--today', inR && 'ts-cal__day--in', (isS || isE) && 'ts-cal__day--edge', isS && end && end !== start && 'ts-cal__day--start', isE && start !== end && 'ts-cal__day--end')}>{d}</button>
    );
  }
  return (
    <div className={cx('ts-cal', className)}>
      <div className="ts-cal__head">
        <span className="ts-cal__month">{MONTHS[month]} {year}</span>
        <IconButton icon="chevron-left" label="Предыдущий месяц" variant="secondary" size="sm" onClick={onPrev} />
        <IconButton icon="chevron-right" label="Следующий месяц" variant="secondary" size="sm" onClick={onNext} />
      </div>
      <div className="ts-cal__grid">
        {WD.map(w => <span key={w} className="ts-cal__wd">{w}</span>)}
        {cells}
      </div>
    </div>
  );
}
