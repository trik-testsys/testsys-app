/**
 * @startingPoint section="Layout" subtitle="Header + stacked rows of blocks" viewport="1440x600"
 */
export interface PageProps {
  /** Usually <Header/> (plus an optional sub-header strip). */
  header?: React.ReactNode;
  /** A vertical stack of <Row/> (and full-width <Block/>s). */
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}
