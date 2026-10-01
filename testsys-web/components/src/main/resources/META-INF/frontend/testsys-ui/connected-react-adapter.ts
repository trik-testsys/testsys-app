import {ReactAdapterElement} from 'Frontend/generated/flow/ReactAdapter';
import {ConnectionLifecycle} from './connection-lifecycle';

export abstract class ConnectedReactAdapterElement extends ReactAdapterElement {
  private readonly lifecycle = new ConnectionLifecycle(
    () => this.isConnected,
    () => super.connectedCallback(),
    () => super.disconnectedCallback()
  );

  connectedCallback(): Promise<void> {
    return this.lifecycle.connected();
  }

  disconnectedCallback(): Promise<void> {
    return this.lifecycle.disconnected();
  }
}
