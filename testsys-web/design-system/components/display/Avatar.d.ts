export interface AvatarProps {
  /** Full name; initials are derived from it. */
  name?: string;
  initials?: string;
  /** 24, 28, 32, 40, 44, 96. */
  size?: number;
  /** 0 info, 1 success, 2 warning, 3 muted, dark. Defaults to a hash of the name. */
  tone?: 0 | 1 | 2 | 3 | 'dark';
  /** Rounded square — teams. */
  square?: boolean;
  className?: string;
  style?: React.CSSProperties;
}
