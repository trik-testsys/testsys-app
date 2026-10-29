import assert from 'node:assert/strict';
import test from 'node:test';
import {ConnectionLifecycle} from '../../main/resources/META-INF/frontend/testsys-ui/connection-lifecycle.ts';

function deferred() {
  let resolve;
  const promise = new Promise(done => { resolve = done; });
  return {promise, resolve};
}

test('reconnect waits for an in-flight root unmount', async () => {
  let connected = true;
  let root = false;
  const unmount = deferred();
  const events = [];
  const lifecycle = new ConnectionLifecycle(() => connected, async () => {
    events.push('connect');
    if (!root) root = true;
  }, async () => {
    events.push('disconnect-start');
    await unmount.promise;
    root = false;
    events.push('disconnect-end');
  });
  await lifecycle.connected();
  connected = false;
  const removed = lifecycle.disconnected();
  await Promise.resolve();
  connected = true;
  const reattached = lifecycle.connected();
  assert.deepEqual(events, ['connect', 'disconnect-start']);
  unmount.resolve();
  await Promise.all([removed, reattached]);
  assert.equal(root, true);
  assert.deepEqual(events, ['connect', 'disconnect-start', 'disconnect-end', 'connect']);
});

test('synchronous reparent skips the stale connection and mounts the current one', async () => {
  let connected = true;
  const events = [];
  const lifecycle = new ConnectionLifecycle(() => connected,
    async () => { events.push('connect'); }, async () => { events.push('disconnect'); });
  const first = lifecycle.connected();
  connected = false;
  const removed = lifecycle.disconnected();
  connected = true;
  const current = lifecycle.connected();
  await Promise.all([first, removed, current]);
  assert.deepEqual(events, ['disconnect', 'connect']);
});

test('detach waits for an in-flight mount and leaves no root behind', async () => {
  let connected = true;
  let root = false;
  const mount = deferred();
  const lifecycle = new ConnectionLifecycle(() => connected, async () => {
    await mount.promise;
    root = true;
  }, async () => { root = false; });
  const attached = lifecycle.connected();
  await Promise.resolve();
  connected = false;
  const removed = lifecycle.disconnected();
  mount.resolve();
  await Promise.all([attached, removed]);
  assert.equal(root, false);
});

test('a failed callback does not block a later connection', async () => {
  let attempts = 0;
  const lifecycle = new ConnectionLifecycle(() => true, async () => {
    if (++attempts === 1) throw new Error('mount failed');
  }, async () => {});
  await assert.rejects(lifecycle.connected(), /mount failed/);
  await lifecycle.connected();
  assert.equal(attempts, 2);
});
