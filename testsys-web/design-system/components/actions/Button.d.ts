import type { IconName } from '../core/Icon';
/**
 * @startingPoint section="Actions" subtitle="Primary, secondary, ghost, danger buttons" viewport="700x220"
 */
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** primary = main action (one per block); secondary = neutral; ghost = low emphasis; link = inline text action; danger / danger-soft = destructive; dark = on light overlays and accent blocks. */
  variant?: 'primary' | 'secondary' | 'ghost' | 'link' | 'danger' | 'danger-soft' | 'dark' | 'success-soft';
  /** sm 32px (tables, block heads), md 40px (default), lg 48px (auth, hero). */
  size?: 'sm' | 'md' | 'lg';
  icon?: IconName;
  iconRight?: IconName;
  /** Shows a spinner and ignores clicks, keeps the variant colour. */
  loading?: boolean;
  block?: boolean;
  disabled?: boolean;
  children?: React.ReactNode;
}
