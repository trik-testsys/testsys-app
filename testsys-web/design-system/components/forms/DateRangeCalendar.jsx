import React from 'react';
import { IconButton } from '../actions/IconButton.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

const MONTHS = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь'];
const WD = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
const iso = (y, m, d) => (y >= 0 && y <= 9999 ? String(y).padStart(4, '0') : (y < 0 ? '-' : '+') + String(Math.abs(y)).padStart(4, '0')) + '-' + String(m + 1).padStart(2, '0') + '-' + String(d).padStart(2, '0');
// Civil days must not shift with browser timezones or Date's special handling of years 0–99.
const civil = (y, m, d) => { const date = new Date(0); date.setUTCFullYear(y, m, d); date.setUTCHours(12, 0, 0, 0); return date; };

export function DateRangeCalendar({ year, month, start, end, today, range = true, onChange, onPrev, onNext, months = MONTHS, weekdays = WD, previousLabel = "Предыдущий месяц", nextLabel = "Следующий месяц", firstDay = 1, locale = "ru-RU", onMonthChange, className }) {
  const offset = (civil(year, month, 1).getUTCDay() - firstDay + 7) % 7;
  const pending = React.useRef(null);
  const root = React.useRef(null);
  const [focusDay, setFocusDay] = React.useState(start || end || today || iso(year, month, 1));
  React.useEffect(() => {
    const target = pending.current;
    if (target) { root.current?.querySelector(`[data-date="${target}"]`)?.focus(); pending.current = null; }
    else if (!focusDay.startsWith(iso(year, month, 1).slice(0, iso(year, month, 1).lastIndexOf('-')+1))) setFocusDay(iso(year, month, 1));
  }, [year, month]);
  const navigate = (e, day) => {
    const date = civil(year, month, day);
    const delta = {ArrowLeft:-1, ArrowRight:1, ArrowUp:-7, ArrowDown:7}[e.key];
    if (delta != null) date.setUTCDate(date.getUTCDate() + delta);
    else if (e.key === 'Home') date.setUTCDate(date.getUTCDate() - ((date.getUTCDay()-firstDay+7)%7));
    else if (e.key === 'End') date.setUTCDate(date.getUTCDate() + 6 - ((date.getUTCDay()-firstDay+7)%7));
    else if (e.key === 'PageUp' || e.key === 'PageDown') { const last = civil(year, month + (e.key === 'PageUp' ? 0 : 2), 0).getUTCDate(); date.setUTCDate(1); date.setUTCMonth(month + (e.key === 'PageUp' ? -1 : 1)); date.setUTCDate(Math.min(day, last)); }
    else return;
    e.preventDefault();
    const target = iso(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()); setFocusDay(target);
    if (date.getUTCMonth() !== month || date.getUTCFullYear() !== year) { pending.current = target; if (onMonthChange) onMonthChange(date.getUTCFullYear(), date.getUTCMonth()); else if (date < civil(year, month, 1)) onPrev?.(); else onNext?.(); }
    else root.current?.querySelector(`[data-date="${target}"]`)?.focus();
  };
  const count = civil(year, month + 1, 0).getUTCDate();
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
    const k = iso(year, month, d);
    const isS = k === start, isE = k === end, inR = start && end && k > start && k < end;
    cells.push(
      <button type="button" key={k} onClick={() => pick(k)} aria-label={civil(year, month, d).toLocaleDateString(locale, { timeZone: 'UTC', year: 'numeric', month: 'long', day: 'numeric' })} aria-pressed={!!(isS || isE)} tabIndex={k === focusDay ? 0 : -1} data-date={k} onFocus={() => setFocusDay(k)} onKeyDown={e => navigate(e, d)} className={cx('ts-cal__day', [0,6].includes(civil(year, month, d).getUTCDay()) && 'ts-cal__day--weekend', k === today && !isS && !isE && !inR && 'ts-cal__day--today', inR && 'ts-cal__day--in', (isS || isE) && 'ts-cal__day--edge', isS && end && end !== start && 'ts-cal__day--start', isE && start !== end && 'ts-cal__day--end')}>{d}</button>
    );
  }
  return (
    <div ref={root} className={cx('ts-cal', className)}>
      <div className="ts-cal__head">
        <span className="ts-cal__month" aria-live="polite">{months[month]} {year}</span>
        <IconButton icon="chevron-left" label={previousLabel} variant="secondary" size="sm" onClick={onPrev} />
        <IconButton icon="chevron-right" label={nextLabel} variant="secondary" size="sm" onClick={onNext} />
      </div>
      <div className="ts-cal__grid">
        {weekdays.map(w => <span key={w} className="ts-cal__wd">{w}</span>)}
        {cells}
      </div>
    </div>
  );
}
