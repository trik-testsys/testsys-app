export interface CodeEditorProps {
  value: string;
  onChange?: (value: string) => void;
  minHeight?: number;
  readOnly?: boolean;
}
