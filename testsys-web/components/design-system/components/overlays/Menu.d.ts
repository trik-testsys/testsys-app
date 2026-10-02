export interface MenuItem { label?: string; value?: string; kbd?: string; danger?: boolean; separator?: boolean; disabled?: boolean; onClick?: () => void; }
export interface MenuProps {
  items: MenuItem[];
  onSelect?: (value: string) => void;
  /** Defaults to an ellipsis IconButton. */
  trigger?: React.ReactNode;
  defaultOpen?: boolean;
  align?: 'left' | 'right';
}
