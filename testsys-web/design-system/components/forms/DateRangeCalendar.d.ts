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
  className?: string;
}
