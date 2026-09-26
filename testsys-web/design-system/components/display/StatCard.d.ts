export interface StatCardProps {
  label: React.ReactNode;
  value: React.ReactNode;
  delta?: React.ReactNode;
  trend?: 'up' | 'down';
  /** Dark accent card (timer, highlight). */
  dark?: boolean;
  /** Grid columns inside a Row (usually 6). */
  span?: number;
  children?: React.ReactNode;
  className?: string;
}
