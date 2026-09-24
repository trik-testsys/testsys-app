export interface RadioProps {
  checked?: boolean;
  onChange?: (checked: true) => void;
  label?: React.ReactNode;
  disabled?: boolean;
  className?: string;
}
