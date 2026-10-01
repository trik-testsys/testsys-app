import React from 'react';
import { Breadcrumbs } from '../navigation/Breadcrumbs.jsx';

export function PageHead({ title, breadcrumbs, badges, meta, actions, tabs, className }) {
  return <div className={['ts-page-head', className].filter(Boolean).join(' ')}>
    <div className="ts-page-head__inner">
      {breadcrumbs ? <Breadcrumbs items={breadcrumbs} /> : null}
      <div className="ts-page-head__title-row">
        <h1 className="ts-h1">{title}</h1>{badges}
        {meta ? <span className="ts-page-head__meta">{meta}</span> : null}
        {actions ? <div className="ts-page-head__actions">{actions}</div> : null}
      </div>{tabs}
    </div>
  </div>;
}
