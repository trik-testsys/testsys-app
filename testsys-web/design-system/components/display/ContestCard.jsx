import React from 'react';
import { StatusBadge } from './StatusBadge.jsx';
import { Tag } from './Tag.jsx';
import { Button } from '../actions/Button.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

const COVER = { info: 'info', live: 'live', warning: 'neutral', neutral: 'neutral', danger: 'neutral', draft: 'neutral' };
export function ContestCard({ title, format, status, statusTone = 'info', when, tags = [], people, cta = 'Участвовать', ctaVariant, onClick, onCta, span, className, style }) {
  const cover = COVER[statusTone] || 'neutral';
  const v = ctaVariant || (cover === 'live' ? 'dark' : cover === 'info' ? 'primary' : 'secondary');
  return (
    <div role={onClick ? 'group' : undefined} aria-label={title} tabIndex={onClick ? 0 : undefined} onKeyDown={e => { if (e.target === e.currentTarget && ['Enter', ' '].includes(e.key)) { e.preventDefault(); onClick?.(); } }} className={cx('ts-ccard', className)} onClick={onClick} style={{ gridColumn: span ? 'span ' + span : undefined, ...style }}>
      <div className={'ts-ccard__cover ts-ccard__cover--' + cover}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span className="ts-ccard__fmt">{format}</span>
          <StatusBadge tone={statusTone} size="sm" className="" >{status}</StatusBadge>
        </div>
        <span className="ts-ccard__when">{when}</span>
      </div>
      <div className="ts-ccard__body">
        <span className="ts-ccard__title">{title}</span>
        {tags.length ? <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>{tags.map(t => <Tag key={t} variant="plain">{t}</Tag>)}</div> : null}
        <div className="ts-ccard__foot">
          <span className="ts-ccard__people">{people}</span>
          <Button size="sm" variant={v} onClick={e => { e.stopPropagation(); onCta && onCta(); }}>{cta}</Button>
        </div>
      </div>
    </div>
  );
}
