import {driver} from 'driver.js';
import 'driver.js/dist/driver.css';
import {installGuide} from './guide-controller';

class GuideController extends HTMLElement {
  previousLabel = '';
  nextLabel = '';
  doneLabel = '';
  closeLabel = '';
  private cleanup?: () => void;

  connectedCallback() {
    this.cleanup?.();
    const root = this.closest<HTMLElement>('.ts-app');
    if (root) this.cleanup = installGuide(root, this, driver);
  }

  disconnectedCallback() {
    this.cleanup?.();
    this.cleanup = undefined;
  }
}

customElements.define('ts-guide-controller', GuideController);
