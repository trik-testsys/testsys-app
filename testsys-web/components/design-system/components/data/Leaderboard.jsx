import React from 'react';
import { Avatar } from '../display/Avatar.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Leaderboard({ problems = [], rows = [], highlightFirst = true }) {
  const cols = '56px minmax(0,1fr) repeat(' + problems.length + ', 64px) 80px 88px';
  return (
    <div style={{ fontFamily: 'var(--font-sans)' }}>
      <div style={{ display: 'grid', gridTemplateColumns: cols, font: '500 12px var(--font-sans)', color: 'var(--text-secondary)', background: 'var(--sunken)', borderBottom: '1px solid var(--border-default)' }}>
        <span style={{ padding: '10px 0 10px 20px' }}>#</span><span style={{ padding: '10px 12px' }}>Участник</span>
        {problems.map(p => <span key={p} className="ts-mono" style={{ padding: '10px 0', textAlign: 'center', fontWeight: 600, color: 'var(--text-primary)' }}>{p}</span>)}
        <span style={{ padding: '10px 0', textAlign: 'center' }}>Решено</span><span style={{ padding: '10px 20px 10px 0', textAlign: 'right' }}>Штраф</span>
      </div>
      {rows.map((r, i) => (
        <div key={r.name} style={{ display: 'grid', gridTemplateColumns: cols, alignItems: 'center', borderBottom: '1px solid var(--line-subtle)', background: highlightFirst && i === 0 ? 'var(--sunken)' : undefined }}>
          <span className="ts-mono" style={{ padding: '12px 0 12px 20px', fontWeight: 600 }}>{r.rank != null ? r.rank : i + 1}</span>
          <div style={{ padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
            <Avatar name={r.name} size={28} tone={3} />
            <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
              <span style={{ font: '600 14px var(--font-sans)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{r.name}</span>
              {r.org ? <span style={{ font: '400 12px var(--font-sans)', color: 'var(--text-secondary)' }}>{r.org}</span> : null}
            </div>
          </div>
          {r.cells.map((c, j) => (
            <div key={j} className={cx('ts-lb-cell', 'ts-lb-cell--' + (c.state || 'none'))}><b>{c.value || (c.state === 'none' || !c.state ? '·' : '')}</b>{c.time ? <small>{c.time}</small> : null}</div>
          ))}
          <span className="ts-mono" style={{ textAlign: 'center', fontWeight: 700, fontSize: 15 }}>{r.solved}</span>
          <span className="ts-mono" style={{ paddingRight: 20, textAlign: 'right', color: 'var(--text-secondary)' }}>{r.penalty}</span>
        </div>
      ))}
    </div>
  );
}
