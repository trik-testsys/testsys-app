function PrototypeDateRange({ from, to, onChange, id, ...fieldAria }) {
  const { Input, IconButton, Popover, DateRangeCalendar } = window.TS;
  const fallbackId = React.useId();
  const invalid = fieldAria['aria-invalid'] === true || fieldAria['aria-invalid'] === 'true';
  const [open, setOpen] = React.useState(false);
  const [month, setMonth] = React.useState(() => ({ year: new Date().getFullYear(), month: new Date().getMonth() }));
  const setOpened = value => {
    if (value) {
      const selected = [from, to].find(date => window.PrototypeTables.isPrototypeIsoDate(date));
      if (selected) { const [year, number] = selected.split('-').map(Number); setMonth({ year, month: number - 1 }); }
      else { const now = new Date(); setMonth({ year: now.getFullYear(), month: now.getMonth() }); }
    }
    setOpen(value);
  };
  const move = delta => setMonth(current => {
    const number = current.month + delta;
    return number < 0 ? { year: current.year - 1, month: 11 } : number > 11 ? { year: current.year + 1, month: 0 } : { ...current, month: number };
  });
  const today = new Date();
  const todayIso = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
  return <div className="ts-hstack" style={{ flexWrap: 'nowrap' }}>
    <span className="ts-muted">С</span><Input {...fieldAria} id={id || fallbackId} error={invalid} mono aria-label="Период: с" placeholder="ГГГГ-ММ-ДД" style={{ flex: '1 1 0', width: 'auto', minWidth: 0 }} value={from} onChange={event => onChange({ from: event.target.value, to })} />
    <span className="ts-muted">До</span><Input {...fieldAria} id={(id || fallbackId) + '-to'} error={invalid} mono aria-label="Период: до" placeholder="ГГГГ-ММ-ДД" style={{ flex: '1 1 0', width: 'auto', minWidth: 0 }} value={to} onChange={event => onChange({ from, to: event.target.value })} />
    <Popover open={open} onOpenChange={setOpened} align="right" width={300} trigger={<IconButton icon="calendar" label="Выбрать период" variant="secondary" />}>
      <div style={{ padding: 'var(--space-3)' }}><DateRangeCalendar year={month.year} month={month.month} start={window.PrototypeTables.isPrototypeIsoDate(from) ? from : null} end={window.PrototypeTables.isPrototypeIsoDate(to) ? to : null} today={todayIso} onPrev={() => move(-1)} onNext={() => move(1)} onMonthChange={(year, number) => setMonth({ year, month: number })} onChange={range => onChange({ from: range.start || '', to: range.end || '' })} /></div>
    </Popover>
  </div>;
}
window.PrototypeDateRange = PrototypeDateRange;
