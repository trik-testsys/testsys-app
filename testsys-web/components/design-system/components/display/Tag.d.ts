export interface TagProps {
  /** default = topic tag; plain = no border; mono = code-like; dark = rating; up / down = rating delta. */
  variant?: 'default' | 'plain' | 'mono' | 'dark' | 'up' | 'down';
  onRemove?: () => void;
  children: React.ReactNode;
  className?: string;
}
