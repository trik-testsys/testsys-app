export interface ProgressBarProps {
  /** 0–100. */
  value?: number;
  /** accent = process; success = solved share; danger = failed at. */
  tone?: 'accent' | 'success' | 'danger';
  /** 4px instead of 6px. */
  thin?: boolean;
  /** Unknown duration (server preparing). */
  indeterminate?: boolean;
  /** On dark blocks. */
  inverse?: boolean;
  className?: string;
  style?: React.CSSProperties;
}
