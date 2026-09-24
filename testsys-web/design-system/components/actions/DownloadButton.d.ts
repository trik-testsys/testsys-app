/**
 * @startingPoint section="Actions" subtitle="Download action with preparing / progress / done / error" viewport="700x220"
 */
export interface DownloadButtonProps {
  /** idle → preparing (server builds the file, unknown duration) → downloading (known progress) → done | error. */
  state?: 'idle' | 'preparing' | 'downloading' | 'done' | 'error';
  /** 0–100, used in downloading. */
  progress?: number;
  size?: 'sm' | 'md' | 'lg';
  /** Square icon version; downloading shows a progress ring with a stop square. */
  iconOnly?: boolean;
  /** Start / retry / re-download. */
  onClick?: () => void;
  /** Called from preparing and downloading. */
  onCancel?: () => void;
  labels?: Partial<Record<'idle' | 'preparing' | 'cancel' | 'done' | 'error', string>>;
  className?: string;
}
