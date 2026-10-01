export interface VerdictProps {
  score?: number;
  /** Application-provided score caption; no success thresholds. */
  label?: React.ReactNode;
  code?: 'ok' | 'wa' | 'tle' | 'mle' | 're' | 'ce' | 'queue';
  /** Adds the Russian description next to the code. */
  showLabel?: boolean;
  /** Override text, e.g. "Тест 7/24" while testing. */
  children?: React.ReactNode;
  className?: string;
}
