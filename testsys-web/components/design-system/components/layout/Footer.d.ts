/** A footer destination supplied by the page. */
export interface FooterLink { label: string; href: string; }
/**
 * @startingPoint section="Layout" subtitle="Brand, current year and page-owned links" viewport="1440x120"
 */
export interface FooterProps {
  /** Accessible name of the TestSys logo. */
  brand?: string;
  /** Links in display order; empty by default, opened in the current tab. */
  links?: FooterLink[];
  /** Accessible name of the link navigation, omitted when there are no links. */
  linksLabel?: string;
}
