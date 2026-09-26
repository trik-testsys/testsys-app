/**
 * @startingPoint section="Layout" subtitle="Block: head, body, footer container" viewport="700x280"
 */
export interface BlockProps {
  /** Columns in the parent Row (1–24). Omit for full width outside a Row. */
  span?: number;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  /** Right side of the head: tabs, filters, buttons. */
  actions?: React.ReactNode;
  footer?: React.ReactNode;
  /** Ink background — timers, highlights. Max one per row. */
  dark?: boolean;
  /** Canvas-deep background without border — for previews and grouping. */
  sunken?: boolean;
  /** No body padding — tables, lists, editors. */
  flush?: boolean;
  bodyStyle?: React.CSSProperties;
  children?: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
