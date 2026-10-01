export interface QuizOptionProps {
  letter?: string;
  label: React.ReactNode;
  /** Square indicator (several answers allowed). */
  multiple?: boolean;
  selected?: boolean;
  /** After checking: correct (green) or wrong (red, only for the picked wrong ones). Locks clicks. */
  result?: 'correct' | 'wrong' | null;
  onClick?: () => void;
}
