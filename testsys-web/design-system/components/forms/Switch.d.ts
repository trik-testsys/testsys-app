export interface SwitchProps {
  checked?: boolean;
  onChange?: (checked: boolean) => void;
  /** With a label the switch sits on the right of a full-width row (settings lists). */
  label?: React.ReactNode;
  disabled?: boolean;
  className?: string;
}
