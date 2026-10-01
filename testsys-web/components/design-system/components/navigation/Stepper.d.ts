export interface StepperProps {
  steps: { label: string; sub?: string }[];
  /** 0-based index of the active step. */
  current: number;
  onStepClick?: (index: number) => void;
}
