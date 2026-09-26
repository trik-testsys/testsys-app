export interface SegmentedControlProps {
  options: Array<{ value: string; label: React.ReactNode } | string>;
  value: string;
  onChange?: (value: string) => void;
  /** sm 22px (block heads), md 26px, lg 30px (auth tabs). */
  size?: 'sm' | 'md' | 'lg';
  /** Stretch to container width. */
  block?: boolean;
  className?: string;
}
