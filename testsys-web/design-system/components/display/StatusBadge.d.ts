export interface StatusBadgeProps {
  /** live = идёт (green, glowing dot); info = регистрация / скоро; warning = заморожено / внимание; neutral = завершено; danger = отклонено; draft = черновик (dashed). */
  tone?: 'live' | 'info' | 'warning' | 'neutral' | 'danger' | 'draft';
  size?: 'sm' | 'md';
  dot?: boolean;
  children: React.ReactNode;
  className?: string;
}
