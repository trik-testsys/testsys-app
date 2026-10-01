export interface SelectOption { value: string; label: React.ReactNode; /** Right-aligned mono hint, e.g. compiler version. */ meta?: React.ReactNode; }
export interface SelectProps {
  id?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
  'aria-invalid'?: boolean;
  options: Array<SelectOption | string>;
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  error?: boolean;
  disabled?: boolean;
  className?: string;
}
