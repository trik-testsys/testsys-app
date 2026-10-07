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
test('request and removal carry identity and lifecycle replaces listeners', () => {
  const host = new Upload(), file = {name:'same.txt'}, headers = [], removed = [];
  const xhr = {setRequestHeader:(name, value) => headers.push([name,value])};
  host.addEventListener('testsys-transfer-remove', event => removed.push(event.detail.identity));
  attachTransfers(host); attachTransfers(host);
  emit(host, 'upload-request', {file,xhr});
  emit(host, 'file-remove', {file});
  assert.equal(headers.length, 1);
  assert.equal(headers[0][0], 'X-TestSys-Transfer');
  assert.deepEqual(removed, [headers[0][1]]);
  host.generation = '1';
  emit(host, 'upload-request', {file,xhr});
  assert.notEqual(headers[0][1], headers[1][1]);
  detachTransfers(host);
  emit(host, 'upload-request', {file,xhr});
  emit(host, 'file-remove', {file});
  assert.equal(headers.length, 2); assert.equal(removed.length, 1);
});
