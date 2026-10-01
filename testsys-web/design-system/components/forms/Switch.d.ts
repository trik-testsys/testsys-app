export interface SwitchProps {
  id?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
  'aria-required'?: boolean;
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  /** With a label the switch sits on the right of a full-width row (settings lists). */
  label?: React.ReactNode;
  disabled?: boolean;
  className?: string;
}
