/**
 * @startingPoint section="Forms" subtitle="Multi-select dropdown with search, chips and select-all" viewport="700x460"
 */
export interface MultiSelectProps {
  id?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
  'aria-invalid'?: boolean;
  options: Array<{ value: string; label: React.ReactNode; meta?: React.ReactNode } | string>;
  value: string[];
  onChange: (value: string[]) => void;
  placeholder?: string;
  /** Trigger text in count mode, e.g. "Языки". */
  label?: string;
  /** chips = removable chips with "+N" overflow; count = label + counter badge. */
  display?: 'chips' | 'count';
  maxChips?: number;
  searchable?: boolean;
  searchPlaceholder?: string;
  showSelectAll?: boolean;
  onApply?: (value: string[]) => void;
  error?: boolean;
  disabled?: boolean;
  defaultOpen?: boolean;
  className?: string;
}
