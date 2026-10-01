export interface AlertProps {
  /** info / warning / danger / success = inline soft messages; dark = announcement banner. */
  tone?: 'info' | 'warning' | 'danger' | 'success' | 'dark';
  title?: React.ReactNode;
  children?: React.ReactNode;
  /** Trailing link/button. */
  action?: React.ReactNode;
  /** Dark tone only: small accent pill, e.g. "Новое". */
  badge?: React.ReactNode;
}
