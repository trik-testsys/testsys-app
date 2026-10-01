import type { BreadcrumbsProps } from '../navigation/Breadcrumbs';
export interface PageHeadProps {
  title: React.ReactNode;
  breadcrumbs?: BreadcrumbsProps['items'];
  badges?: React.ReactNode;
  meta?: React.ReactNode;
  actions?: React.ReactNode;
  tabs?: React.ReactNode;
  className?: string;
}
