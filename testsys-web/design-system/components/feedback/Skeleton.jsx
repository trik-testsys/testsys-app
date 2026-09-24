import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Skeleton({ width = '100%', height = 12, circle = false, style }) {
  return <span className={cx('ts-skel', circle && 'ts-skel--circle')} style={{ display: 'block', width, height, ...style }} />;
}
export function SkeletonRows({ rows = 5 }) {
  const w = [[62, 40], [80, 30], [55, 45], [72, 25], [66, 38]];
  return (
    <div>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="ts-list-row" style={{ display: 'grid', gridTemplateColumns: '28px 1fr 80px 64px', gap: 16 }}>
          <Skeleton circle width={28} height={28} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}><Skeleton width={w[i % 5][0] + '%'} height={10} /><Skeleton width={w[i % 5][1] + '%'} height={8} /></div>
          <Skeleton height={10} /><Skeleton height={22} style={{ borderRadius: 6 }} />
        </div>
      ))}
    </div>
  );
}
