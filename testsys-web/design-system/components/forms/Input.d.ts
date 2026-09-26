export interface InputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size' | 'prefix'> {
  /** md 32px default; lg 38px for auth forms. */
  size?: 'md' | 'lg';
  error?: boolean;
  /** JetBrains Mono for codes, IDs, numbers. */
  mono?: boolean;
  /** Leading adornment (usually an <Icon/>). */
  prefix?: React.ReactNode;
  /** Trailing unit cell, e.g. "минут". */
  suffix?: React.ReactNode;
}
