export interface DrawerProps {
  open?: boolean;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  children?: React.ReactNode;
  footer?: React.ReactNode;
  onClose?: () => void;
  /** Default 480. */
  width?: number;
  /** Position inside the nearest positioned parent (previews). */
  contained?: boolean;
}
