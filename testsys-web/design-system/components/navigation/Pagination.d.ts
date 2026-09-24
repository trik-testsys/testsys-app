export interface PaginationProps {
  page: number;
  total: number;
  onChange?: (page: number) => void;
  /** "‹ 3 / 24 ›" for narrow block footers. */
  compact?: boolean;
}
