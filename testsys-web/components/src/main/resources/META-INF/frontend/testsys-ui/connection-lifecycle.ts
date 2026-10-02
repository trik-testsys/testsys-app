// Flow 25.3 disconnects its React root after an await, but reconnect checks that root before awaiting the disconnect.
// Serialize complete base callbacks so a popup's synchronous reparent cannot leave the new connection unmounted.
export class ConnectionLifecycle {
  private pending: Promise<void> = Promise.resolve();
  private generation = 0;
  private readonly isConnected: () => boolean;
  private readonly connect: () => Promise<void>;
  private readonly disconnect: () => Promise<void>;

  constructor(isConnected: () => boolean, connect: () => Promise<void>, disconnect: () => Promise<void>) {
    this.isConnected = isConnected;
    this.connect = connect;
    this.disconnect = disconnect;
  }

  connected(): Promise<void> {
    const generation = ++this.generation;
    return this.enqueue(async () => {
      if (generation === this.generation && this.isConnected()) await this.connect();
    });
  }

  disconnected(): Promise<void> {
    ++this.generation;
    return this.enqueue(this.disconnect);
  }

  private enqueue(action: () => Promise<void>): Promise<void> {
    const operation = this.pending.then(action, action);
    this.pending = operation;
    return operation;
  }
}
