export interface SelectOption { value: string; label: React.ReactNode; /** Right-aligned mono hint, e.g. compiler version. */ meta?: React.ReactNode; }
export interface SelectProps {
  options: Array<SelectOption | string>;
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  error?: boolean;
  disabled?: boolean;
  className?: string;
}
