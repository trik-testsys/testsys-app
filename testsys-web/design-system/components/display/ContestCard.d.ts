/**
 * @startingPoint section="Display" subtitle="Contest / olympiad / quiz card for catalogs" viewport="700x320"
 */
export interface ContestCardProps {
  title: string;
  /** "ICPC · 5 ч" — rendered uppercase mono. */
  format: string;
  status: string;
  /** info = registration (cream cover); live = running (dark cover); neutral = finished. */
  statusTone?: 'info' | 'live' | 'warning' | 'neutral';
  /** Large mono line: "через 2 д", live countdown, date. */
  when: React.ReactNode;
  tags?: string[];
  people?: React.ReactNode;
  cta?: string;
  ctaVariant?: 'primary' | 'secondary' | 'dark';
  onClick?: () => void;
  onCta?: () => void;
  /** Grid columns inside a Row (8 = three per row). */
  span?: number;
  className?: string;
  style?: React.CSSProperties;
}
