/**
 * @startingPoint section="Layout" subtitle="Header + stacked rows of blocks" viewport="1440x600"
 */
export interface PageProps {
  /** Usually <Header/> (plus an optional sub-header strip). */
  header?: React.ReactNode;
  /** PageHead between Header and main. */
  head?: React.ReactNode;
  /** Page footer; defaults to the branded Footer with the current year and no links. */
  footer?: React.ReactNode;
  /** A vertical stack of <Row/> (and full-width <Block/>s). */
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
