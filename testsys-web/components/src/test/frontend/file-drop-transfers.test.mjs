import assert from 'node:assert/strict';
import test from 'node:test';
globalThis.window = {};
const {attachTransfers, detachTransfers, transferIdentity} = await import('../../main/resources/META-INF/frontend/testsys-ui/file-drop-transfers.ts');
class Upload extends EventTarget { generation = '0'; getAttribute() { return this.generation; } }
const emit = (host, name, detail) => host.dispatchEvent(new CustomEvent(name, {detail}));
test('same named files have distinct stable identities through retry', () => {
  const first = {name:'same.txt'}, second = {name:'same.txt'};
  assert.equal(transferIdentity(first), transferIdentity(first));
  assert.notEqual(transferIdentity(first), transferIdentity(second));
});
test('identity is a lowercase UUID v4 accepted by the server pattern', () => {
  assert.match(transferIdentity({name:'file.txt'}), /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
});
test('identity does not need the secure-context randomUUID', (t) => {
  t.mock.method(crypto, 'randomUUID', () => { throw new TypeError('randomUUID is unavailable outside secure contexts'); });
  assert.match(transferIdentity({name:'plain-http.txt'}), /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
});
function transfers() {
  const host = new Upload(), file = {name:'same.txt'}, headers = [], removed = [];
  const xhr = {setRequestHeader:(name, value) => headers.push([name,value])};
  host.addEventListener('testsys-transfer-remove', event => removed.push(event.detail.identity));
  return {
    host, headers, removed,
    request: () => emit(host, 'upload-request', {file,xhr}),
    remove: () => emit(host, 'file-remove', {file}),
  };
}
test('repeated attach sends one identity header with a request', () => {
  const {host, headers, request} = transfers();
  attachTransfers(host); attachTransfers(host);
  request();
  assert.equal(headers.length, 1);
  assert.equal(headers[0][0], 'X-TestSys-Transfer');
});
test('removal reports the identity sent with the request', () => {
  const {host, headers, removed, request, remove} = transfers();
  attachTransfers(host);
  request();
  remove();
  assert.deepEqual(removed, [headers[0][1]]);
});
test('a new generation gives the same file a new identity', () => {
  const {host, headers, request} = transfers();
  attachTransfers(host);
  request();
  host.generation = '1';
  request();
  assert.notEqual(headers[0][1], headers[1][1]);
});
test('detach stops identifying requests', () => {
  const {host, headers, request} = transfers();
  attachTransfers(host);
  detachTransfers(host);
  request();
  assert.equal(headers.length, 0);
});
test('detach stops reporting removals', () => {
  const {host, removed, request, remove} = transfers();
  attachTransfers(host);
  request();
  detachTransfers(host);
  remove();
  assert.equal(removed.length, 0);
});
