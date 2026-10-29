import type {ReactElement} from 'react';

export interface DateRangeCalendarProps {
  year: number;
  /** 0-based month. */
  month: number;
  /** ISO "YYYY-MM-DD". */
  start: string | null;
  end: string | null;
  /** ISO date outlined as today. */
  today: string;
  onChange: (v: { start: string | null; end: string | null }) => void;
  onPrev: () => void;
  onNext: () => void;
  months: string[];
  weekdays: string[];
  previousLabel: string;
  nextLabel: string;
  firstDay: number;
  locale: string;
  /** Keyboard navigation into a neighbouring month. */
  onMonthChange: (year: number, month: number) => void;
}

export function DateRangeCalendar(props: DateRangeCalendarProps): ReactElement;
