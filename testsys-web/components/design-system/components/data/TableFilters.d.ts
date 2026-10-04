/**
 * @startingPoint section="Data" subtitle="Ordinary form fields with explicit Apply and Reset" viewport="1000x460"
 */
export interface TableFiltersProps {
  title?: string;
  applyLabel?: string;
  resetLabel?: string;
  /** Same column count as the containing block; fields use the ordinary BlockRow and Field. */
  columns?: number;
  /** Validate draft values and publish an applied snapshot; false prevents refreshing. */
  onApply: () => boolean;
  /** Restore both draft and applied values to the page defaults. */
  onReset: () => void;
  /** Refresh the first table page, exactly once after valid application or resetting. */
  onRefresh: () => void;
  children: React.ReactNode;
}
