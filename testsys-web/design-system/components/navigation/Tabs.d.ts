export interface TabItem { value: string; label: React.ReactNode; count?: number; countTone?: 'muted' | 'danger' | 'dark'; }
export interface TabsProps {
  items: Array<TabItem | string>;
  value: string;
  onChange?: (value: string) => void;
  /** lg = 52px, for block heads and page sub-headers. */
  size?: 'md' | 'lg';
  /** No bottom rule (when the parent already draws one). */
  bare?: boolean;
  className?: string;
}
