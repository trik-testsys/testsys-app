export type IconName = 'search' | 'chevron-down' | 'chevron-up' | 'chevron-left' | 'chevron-right' | 'bell' | 'plus' | 'minus' | 'check' | 'x' | 'upload' | 'download' | 'clock' | 'file' | 'info' | 'triangle-alert' | 'circle-x' | 'circle-check' | 'ellipsis' | 'list-filter' | 'calendar' | 'flag' | 'grip-vertical' | 'help-circle' | 'refresh-cw' | 'external-link' | 'trash' | 'pencil' | 'user' | 'users' | 'trophy' | 'code' | 'lock' | 'megaphone' | 'snowflake' | 'log-out' | 'settings';
export interface IconProps extends React.SVGProps<SVGSVGElement> {
  /** Lucide icon name (subset bundled with the system). */
  name: IconName;
  /** Pixel size. 14 inside S controls, 16 default, 18 in header, 20–22 in empty states. */
  size?: number;
  /** Stroke width; 2 default, 1.8 for 18px+. */
  strokeWidth?: number;
}
