export interface TimerProps {
  /** Target timestamp (ms). Counts down live. */
  to?: number;
  /** Static remaining seconds (when not live). */
  seconds?: number;
  /** chip = compact pill; hero = 56px parts for dark blocks; tiles = days/hours/min/sec for "до начала"; text = inline mono. */
  variant?: 'chip' | 'hero' | 'tiles' | 'text';
  /** Chip turns red with a pulsing dot below this many seconds. Default 600. */
  dangerBelow?: number;
  className?: string;
}
