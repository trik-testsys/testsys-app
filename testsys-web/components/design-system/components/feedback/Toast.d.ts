export interface ToastProps {
  tone?: 'success' | 'error' | 'info' | 'warning';
  title: React.ReactNode;
  description?: React.ReactNode;
  onClose?: () => void;
}
