import React from 'react';
import {ConnectedReactAdapterElement} from './connected-react-adapter';
import { type RenderHooks } from 'Frontend/generated/flow/ReactAdapter';
import { ServerSnapshot } from './server-snapshot';
import { DateRangeCalendar } from './DateRangeCalendar.jsx';
function viewMonth(value?: string) {
  const parts = value?.match(/^([+-]?\d+)-(\d{2})-\d{2}$/);
  if (parts) return {year: Number(parts[1]), month: Number(parts[2])-1};
  const now = new Date();
  return {year: now.getFullYear(), month: now.getMonth()};
}
class CalendarAdapter extends ConnectedReactAdapterElement {
  private readonly snapshot = new ServerSnapshot<any>(this, 'calendar');
  protected render(hooks: RenderHooks) {
    const data = this.snapshot.useSnapshot();
    const changed = hooks.useCustomEvent<any>('range-pick');
    const anchor = data?.start ?? data?.end ?? data?.today;
    const [month, setMonth] = React.useState(() => viewMonth(anchor));
    React.useEffect(() => { if (anchor) setMonth(viewMonth(anchor)); }, [anchor]);
    if (!data) return null;
    const move = (delta: number) => setMonth(current => ({year: current.year + Math.floor((current.month+delta)/12), month: (current.month+delta+12)%12}));
    return <DateRangeCalendar year={month.year} month={month.month} {...data}
      onMonthChange={(year: number, month: number) => setMonth({year, month})}
      onChange={changed} onPrev={() => move(-1)} onNext={() => move(1)} />;
  }
}
customElements.define('testsys-date-range-calendar', CalendarAdapter);
