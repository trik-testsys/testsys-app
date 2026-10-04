// Civil dates must not shift with browser timezones or Date's special handling of years 0–99.
export function civilDate(year: number, month: number, day: number): Date {
  const date = new Date(0);
  date.setUTCFullYear(year, month, day);
  date.setUTCHours(12, 0, 0, 0);
  return date;
}

export function calendarIso(year: number, month: number, day: number): string {
  const prefix = year >= 0 && year <= 9999
    ? String(year).padStart(4, '0')
    : (year < 0 ? '-' : '+') + String(Math.abs(year)).padStart(4, '0');
  return prefix + '-' + String(month + 1).padStart(2, '0') + '-' + String(day).padStart(2, '0');
}

// Compare civil parts directly: signed ISO years are not lexicographically ordered.
export function compareCalendarDates(left: string, right: string): number {
  const parts = (value: string) => {
    const matched = /^([+-]?\d+)-(\d{2})-(\d{2})$/.exec(value);
    if (!matched) throw new Error('Invalid civil calendar date');
    return matched.slice(1).map(Number);
  };
  const a = parts(left), b = parts(right);
  for (let index = 0; index < a.length; index++) {
    if (a[index] !== b[index]) return Math.sign(a[index] - b[index]);
  }
  return 0;
}

export function pickCalendarDate(start: string | null, end: string | null, day: string) {
  return !start || end || compareCalendarDates(day, start) < 0
    ? {start:day, end:null} : {start, end:day};
}

export function inCalendarRange(day: string, start: string | null, end: string | null): boolean {
  return !!start && !!end && compareCalendarDates(day, start) > 0 && compareCalendarDates(day, end) < 0;
}
