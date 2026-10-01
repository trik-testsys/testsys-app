export interface QuestionNavProps {
  total: number;
  /** 1-based. */
  current: number;
  answered?: number[];
  flagged?: number[];
  onSelect?: (n: number) => void;
}
