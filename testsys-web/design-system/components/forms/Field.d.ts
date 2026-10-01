export interface FieldProps {
  label?: React.ReactNode;
  /** Helper text under the control. Replaced by error when present. */
  hint?: React.ReactNode;
  error?: React.ReactNode;
  /** Right side of the label row, e.g. a "Забыли пароль?" link. */
  aside?: React.ReactNode;
  disabled?: boolean;
  /** Together with size enables horizontal subgrid layout. */
  labelSize?: number;
  /** Control columns; total field span is labelSize + size. */
  size?: number;
  controlId?: string;
  required?: boolean;
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
