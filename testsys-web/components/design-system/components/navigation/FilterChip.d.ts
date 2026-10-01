export interface FilterChipProps {
  selected?: boolean;
  onChange?: (selected: boolean) => void;
  /** Shows a chevron — opens a Select/MultiSelect popover instead of toggling. */
  dropdown?: boolean;
  children: React.ReactNode;
  className?: string;
}
