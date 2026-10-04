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
