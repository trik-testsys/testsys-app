export type ArrivalClock = {
  now(): number;
  schedule(action: () => void, delay: number): number;
  cancel(id: number): void;
};
const browserClock: ArrivalClock = {
  now: () => performance.now(),
  schedule: (action, delay) => window.setTimeout(action, delay),
  cancel: id => window.clearTimeout(id)
};

/** Counts only unpaused time; overlapping pause reasons release independently. */
export class ArrivalTimer {
  private remaining = 0;
  private started = 0;
  private timer: number | undefined;
  private paused = new Set<string>();
  private expire: () => void;
  private clock: ArrivalClock;

  constructor(expire: () => void, clock: ArrivalClock = browserClock) {
    this.expire = expire;
    this.clock = clock;
  }
  start(duration: number, reasons: string[] = []) {
    this.cancel();
    this.remaining = duration;
    reasons.forEach(reason => this.paused.add(reason));
    this.arm();
  }
  pause(reason: string) {
    if (this.paused.has(reason)) return;
    if (this.timer !== undefined) {
      this.remaining = Math.max(0, this.remaining - (this.clock.now() - this.started));
      this.clock.cancel(this.timer);
      this.timer = undefined;
    }
    this.paused.add(reason);
  }
  resume(reason: string) {
    if (this.paused.delete(reason)) this.arm();
  }
  cancel() {
    if (this.timer !== undefined) this.clock.cancel(this.timer);
    this.timer = undefined;
    this.remaining = 0;
    this.paused.clear();
  }
  private arm() {
    if (this.paused.size || this.timer !== undefined || this.remaining <= 0) return;
    this.started = this.clock.now();
    this.timer = this.clock.schedule(() => {
      this.timer = undefined;
      this.remaining = 0;
      this.expire();
    }, this.remaining);
  }
}
