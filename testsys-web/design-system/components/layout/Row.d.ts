export interface RowProps {
  /** start (default) or stretch — equal heights for metric rows. */
  align?: 'start' | 'stretch';
  /** 16px gap instead of 24px. */
  tight?: boolean;
  /** Blocks whose span values sum to 12. */
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
export interface StackProps {
  /** Grid columns this vertical stack occupies in its Row. */
  span?: number;
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
