const active = new WeakMap<EventTarget, () => void>();
const identities = new WeakMap<object, string>();
const requests = new WeakMap<object, string>();
const HEADER = 'X-TestSys-Transfer';

export function transferIdentity(file: object): string {
  let identity = identities.get(file);
  if (!identity) { identity = crypto.randomUUID(); identities.set(file, identity); }
  return identity;
}

export function attachTransfers(host: HTMLElement) {
  detachTransfers(host);
  const request = (event: Event) => {
    const {file, xhr} = (event as CustomEvent).detail;
    const identity = host.getAttribute('data-ts-upload-generation') + ':' + transferIdentity(file);
    requests.set(file, identity);
    xhr.setRequestHeader(HEADER, identity);
  };
  const remove = (event: Event) => {
    const file = (event as CustomEvent).detail.file;
    const identity = requests.get(file);
    if (identity) host.dispatchEvent(new CustomEvent('testsys-transfer-remove', {detail:{identity}}));
  };
  host.addEventListener('upload-request', request);
  host.addEventListener('file-remove', remove);
  active.set(host, () => {
    host.removeEventListener('upload-request', request);
    host.removeEventListener('file-remove', remove);
  });
}

export function detachTransfers(host: EventTarget) { active.get(host)?.(); active.delete(host); }

(window as any).testsysFileTransfers = {attach:attachTransfers, detach:detachTransfers};
