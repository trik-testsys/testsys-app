import React from 'react';

// Flow 25.3's generated useState setter dispatches its captured initial value, and its reducer writes a literal
// "key" property. Keep server snapshots outside that SDK state; useContent/useCustomEvent remain the SDK bridge.
export class ServerSnapshot<T> {
  private current: T | undefined;
  private readonly listeners = new Set<() => void>();

  constructor(owner: HTMLElement, key: string) {
    this.current = (owner as any)[key];
    Object.defineProperty(owner, key, {
      configurable: true,
      enumerable: true,
      get: () => this.current,
      set: (next: T) => {
        if (Object.is(this.current, next)) return;
        this.current = next;
        this.listeners.forEach(notify => notify());
      }
    });
  }

  readonly read = () => this.current;
  readonly subscribe = (notify: () => void) => {
    this.listeners.add(notify);
    return () => { this.listeners.delete(notify); };
  };

  useSnapshot(): T | undefined {
    return React.useSyncExternalStore(this.subscribe, this.read, this.read);
  }
}
