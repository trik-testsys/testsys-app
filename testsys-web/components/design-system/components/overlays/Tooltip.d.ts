export interface TooltipProps {
  content: React.ReactNode;
  placement?: 'top' | 'bottom' | 'left' | 'right';
  /** Force visible. */
  open?: boolean;
  /** Render just the bubble (docs). */
  static?: boolean;
  /** Trigger; tooltip shows on hover. */
  children?: React.ReactNode;
}
