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
