import type {ReactElement, ReactNode} from 'react';

export interface SortableListProps<T = any> {
  items: T[];
  /** Stable key per item. Provided by the keyed Flow adapter. */
  getKey: (item: T, index: number) => string | number;
  /** Called on drop with the reordered array (only if the position changed). */
  onChange: (items: T[]) => void;
  /** Row content right of the drag handle. index is the live position — use it for letters (A, B, C…). */
  renderItem: (item: T, index: number) => ReactNode;
  disabled: boolean;
  renderDragItem: (item: T) => ReactNode;
  handleLabel: string;
  announcement: (item: T, position: number, total: number) => string;
}

export function SortableList<T>(props: SortableListProps<T>): ReactElement;
