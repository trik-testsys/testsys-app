export interface CodeEditorProps {
  id?: string;
  label?: string;
  value: string;
  onChange?: (value: string) => void;
  minHeight?: number;
  readOnly?: boolean;
}
