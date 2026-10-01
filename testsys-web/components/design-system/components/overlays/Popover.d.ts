export interface PopoverProps {
  /** Element that toggles the popover on click. */
  trigger: React.ReactNode;
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  align?: 'left' | 'right';
  width?: number | string;
  children: React.ReactNode;
}
