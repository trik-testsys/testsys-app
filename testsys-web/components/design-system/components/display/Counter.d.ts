export interface CounterProps {
  /** danger = unread / needs action; dark = neutral count; accent = selection count; muted = totals. */
  tone?: 'danger' | 'dark' | 'accent' | 'muted';
  children: React.ReactNode;
}
