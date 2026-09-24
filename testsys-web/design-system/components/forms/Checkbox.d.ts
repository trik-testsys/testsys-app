export interface CheckboxProps {
  checked?: boolean;
  /** Tri-state for "select all" rows. */
  indeterminate?: boolean;
  onChange?: (checked: boolean) => void;
  label?: React.ReactNode;
  disabled?: boolean;
  className?: string;
}
