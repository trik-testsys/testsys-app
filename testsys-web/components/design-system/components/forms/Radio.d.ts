export interface RadioProps {
  id?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
  'aria-required'?: boolean;
  checked?: boolean;
  onChange?: (checked: true) => void;
  label?: React.ReactNode;
  disabled?: boolean;
  className?: string;
}
