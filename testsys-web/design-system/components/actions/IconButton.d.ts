import type { IconName } from '../core/Icon';
export interface IconButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  icon: IconName;
  /** Accessible label, also shown as native title. */
  label: string;
  variant?: 'ghost' | 'secondary' | 'primary' | 'dark' | 'danger';
  size?: 'sm' | 'md' | 'lg';
}
