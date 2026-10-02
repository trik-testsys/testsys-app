export interface FileDropProps {
  id?: string;
  'aria-label'?: string;
  'aria-describedby'?: string;
  'aria-invalid'?: boolean;
  /** empty (default dashed), drag (hover over), uploading (file row + progress), done, error. Drag is also detected automatically. */
  state?: 'empty' | 'drag' | 'uploading' | 'done' | 'error';
  title?: string;
  hint?: string;
  fileName?: string;
  /** Short badge text: CPP, ZIP, PDF. */
  fileType?: string;
  /** "2.6 из 4.2 КБ" */
  fileMeta?: string;
  progress?: number;
  error?: string;
  accept?: string;
  multiple?: boolean;
  onSelect?: (files: File[]) => void;
  minHeight?: number;
  className?: string;
}
