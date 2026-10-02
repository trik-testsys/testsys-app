export interface BreadcrumbsProps {
  items: { label: React.ReactNode; href?: string; onClick?: (e: React.MouseEvent) => void }[];
}
