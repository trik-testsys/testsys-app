export interface PillTabsProps {
  items: Array<{ value: string; label: React.ReactNode } | string>;
  value: string;
  onChange?: (value: string) => void;
  className?: string;
}
