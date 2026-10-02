export interface DateRangeCalendarProps {
  year: number;
  /** 0-based month. */
  month: number;
  /** ISO "YYYY-MM-DD". */
  start?: string | null;
  end?: string | null;
  /** ISO date outlined as today. */
  today?: string;
  /** false = single date. */
  range?: boolean;
  onChange?: (v: { start: string | null; end: string | null }) => void;
  onPrev?: () => void;
  onNext?: () => void;
  months?: string[];
  weekdays?: string[];
  previousLabel?: string;
  nextLabel?: string;
  firstDay?: number;
  locale?: string;
  /** Keyboard navigation into a neighbouring month. */
  onMonthChange?: (year: number, month: number) => void;
  className?: string;
}
