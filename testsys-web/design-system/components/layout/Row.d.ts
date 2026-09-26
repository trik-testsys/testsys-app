export interface RowProps {
  /** start (default) or stretch — equal heights for metric rows. */
  align?: 'start' | 'stretch';
  /** 8px gap instead of 12px. */
  tight?: boolean;
  /** Slots whose span values take at most 24 in total (older screens: Blocks and Stacks). */
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
export interface SlotProps {
  /** Columns of the parent Row (1–24); slots of a row take at most 24 in total. */
  span: number;
  /** Stacked SlotRow elements on the columns of the slot. */
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
export interface SlotRowProps {
  /** Blocks whose span values take at most the columns of the slot. */
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
