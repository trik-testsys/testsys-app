export interface DataTableColumn<T = any> {
  key: string;
  title: React.ReactNode;
  /** Pixels, or a width by what the column holds: `narrow` IDs, scores, dates; `medium` names, statuses; `wide` titles; `fill` the rest of the row. */
  width?: number | 'narrow' | 'medium' | 'wide' | 'fill';
  align?: 'right' | 'center';
  /** JetBrains Mono cell (IDs, times, numbers). */
  mono?: boolean;
  /** Secondary ink. */
  muted?: boolean;
  sortable?: boolean;
  render?: (row: T) => React.ReactNode;
}
/**
 * @startingPoint section="Data" subtitle="Selectable, sortable data table" viewport="700x320"
 */
export interface DataTableProps<T = any> {
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey?: string;
  selectable?: boolean;
  selected?: Array<string | number>;
  onSelectChange?: (keys: Array<string | number>) => void;
  sort?: { key: string; dir: 'asc' | 'desc' };
  onSortChange?: (sort: { key: string; dir: 'asc' | 'desc' }) => void;
  compact?: boolean;
  onRowClick?: (row: T) => void;
  /** Rendered in a full-width cell when rows is empty (e.g. <EmptyState/>). */
  empty?: React.ReactNode;
  /** Row keys that just arrived — briefly highlighted in cream (live submissions). */
  newKeys?: Array<string | number>;
}
