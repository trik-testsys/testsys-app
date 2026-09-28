import React from 'react';
const cx = (...a) => a.filter(Boolean).join(' ');

export function Skeleton({ width = '100%', height = 12, circle = false, style }) {
  return <span className={cx('ts-skel', circle && 'ts-skel--circle')} style={{ width, height, ...style }} />;
}
export function SkeletonRows({ rows = 5 }) {
  const w = [[62, 40], [80, 30], [55, 45], [72, 25], [66, 38]];
  return (
    <div aria-hidden="true">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="ts-list-row ts-skel-row">
          <Skeleton circle width={28} height={28} />
          <div className="ts-skel-row__lines"><Skeleton width={w[i % 5][0] + '%'} height={10} /><Skeleton width={w[i % 5][1] + '%'} height={8} /></div>
          <Skeleton height={10} /><span className="ts-skel ts-skel--badge" style={{ height: 22 }} />
        </div>
      ))}
    </div>
  );
}
