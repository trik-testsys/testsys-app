import React from 'react';
import { Icon } from '../core/Icon.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

const pad = n => String(Math.max(0, n)).padStart(2, '0');
export function useCountdown(to) {
  const [now, setNow] = React.useState(Date.now());
  React.useEffect(() => { const t = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(t); }, []);
  return Math.max(0, Math.floor((to - now) / 1000));
}
export function Timer({ to, seconds, variant = 'chip', dangerBelow = 600, className }) {
  const live = useCountdown(to || 0);
  const s = to ? live : Math.max(0, seconds || 0);
  const d = Math.floor(s / 86400), h = Math.floor(s / 3600) % 24, H = Math.floor(s / 3600), m = Math.floor(s / 60) % 60, sec = s % 60;
  if (variant === 'hero') return (
    <div className={cx('ts-timer ts-timer__parts', className)}>
      {[[pad(H), 'часы'], [pad(m), 'минуты'], [pad(sec), 'секунды']].map(([v, u]) => <div key={u} className="ts-timer__part"><span className="ts-timer__num">{v}</span><span className="ts-timer__unit">{u}</span></div>)}
    </div>
  );
  if (variant === 'tiles') return (
    <div className={cx('ts-timer__tiles', className)}>
      {[[pad(d), 'дни'], [pad(h), 'часы'], [pad(m), 'мин'], [pad(sec), 'сек']].map(([v, u]) => <div key={u} className="ts-timer__tile"><b>{v}</b><span>{u}</span></div>)}
    </div>
  );
  const str = pad(H) + ':' + pad(m) + ':' + pad(sec);
  if (variant === 'text') return <span className={cx('ts-timer', className)}>{str}</span>;
  const danger = s <= dangerBelow;
  return <span className={cx('ts-timer ts-timer--chip', danger && 'ts-timer--danger', className)}>{danger ? null : <Icon name="clock" size={14} />}{H ? str : pad(m) + ':' + pad(sec)}</span>;
}
