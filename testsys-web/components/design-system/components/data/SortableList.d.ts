/**
 * @startingPoint section="Data" subtitle="Drag-to-reorder list (problem order)" viewport="700x340"
 */
export interface SortableListProps<T = any> {
  items: T[];
  /** Stable key per item. Defaults to item.id or index. */
  getKey?: (item: T, index: number) => string | number;
  /** Called on drop with the reordered array (only if the position changed). */
  onChange?: (items: T[]) => void;
  /** Row content right of the drag handle. index is the live position — use it for letters (A, B, C…). */
  renderItem?: (item: T, index: number) => React.ReactNode;
  /** Gap between rows, px. Default 8. */
  gap?: number;
  disabled?: boolean;
  renderDragItem?: (item: T) => React.ReactNode;
  handleLabel?: string;
  announcement?: (item: T, position: number, total: number) => string;
  className?: string;
}
