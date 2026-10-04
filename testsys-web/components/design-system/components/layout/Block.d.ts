/**
 * @startingPoint section="Layout" subtitle="Block: head, body, footer container" viewport="700x280"
 */
export interface BlockProps {
  /** Columns of the parent SlotRow; legacy placement directly in Row is supported. Omit for a page block. */
  span?: number;
  /** Continue the page columns through the body and BlockRow children. */
  grid?: boolean;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  /** Right side of the head: tabs, filters, buttons. */
  actions?: React.ReactNode;
  /** TableFilters placed after the head and before the body. */
  filters?: React.ReactNode;
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

export interface BlockRowProps {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
