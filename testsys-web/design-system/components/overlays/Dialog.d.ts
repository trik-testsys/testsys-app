/**
 * @startingPoint section="Overlays" subtitle="Confirm and form dialogs" viewport="700x420"
 */
export interface DialogProps {
  open?: boolean;
  title: React.ReactNode;
  children?: React.ReactNode;
  /** Right-aligned buttons: secondary cancel + primary/danger action. */
  footer?: React.ReactNode;
  /** Backdrop click and Esc. */
  onClose?: () => void;
  /** danger = destructive confirmation with warning glyph and no head rule. */
  variant?: 'default' | 'danger';
  /** sm 440px, md 520px (forms). */
  size?: 'sm' | 'md';
  /** Position inside the nearest positioned parent (previews). */
  contained?: boolean;
}
