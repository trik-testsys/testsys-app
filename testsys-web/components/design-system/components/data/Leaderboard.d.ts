export interface LeaderboardCell {
  /** first = first to solve (solid green); ok = solved; fail = wrong attempts only; frozen = attempt after freeze; none = not tried. */
  state?: 'first' | 'ok' | 'fail' | 'frozen' | 'none';
  /** "+", "+2", "−3", "?1". */
  value?: string;
  /** "01:12" */
  time?: string;
}
export interface LeaderboardRow { rank?: number; name: string; org?: string; solved: number; penalty: number | string; cells: LeaderboardCell[]; }
/**
 * @startingPoint section="Data" subtitle="ICPC standings with per-problem cells" viewport="900x360"
 */
export interface LeaderboardProps {
  /** Problem letters, e.g. ['A','B','C']. */
  problems: string[];
  rows: LeaderboardRow[];
  highlightFirst?: boolean;
}
