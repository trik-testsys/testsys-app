export interface EmptyStateProps {
  icon?: import('../core/Icon').IconName;
  title: React.ReactNode;
  description?: React.ReactNode;
  action?: React.ReactNode;
  /** Load-failure variant (red icon). */
  error?: boolean;
}
