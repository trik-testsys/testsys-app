export interface SegmentedControlProps {
  options: Array<{ value: string; label: React.ReactNode } | string>;
  value: string;
  onChange?: (value: string) => void;
  /** sm 28px (block heads), md 32px, lg 36px (auth tabs). */
  size?: 'sm' | 'md' | 'lg';
  /** Stretch to container width. */
  block?: boolean;
  className?: string;
}
