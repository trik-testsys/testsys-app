import assert from 'node:assert/strict';
import test from 'node:test';
import {ArrivalTimer} from '../../main/resources/META-INF/frontend/testsys-ui/arrival-timer.ts';

function clock() {
  let time = 0;
  let next = 0;
  const pending = new Map();
  return {
    now: () => time,
    schedule: (action, delay) => { const id = ++next; pending.set(id, {action, at: time + delay}); return id; },
    cancel: id => pending.delete(id),
    advance(delta) {
      time += delta;
      for (const [id, entry] of pending) {
        if (entry.at <= time) { pending.delete(id); entry.action(); }
      }
    }
  };
}

test('arrival expires after six seconds of unpaused time', () => {
  const time = clock();
  let expired = 0;
  const timer = new ArrivalTimer(() => expired++, time);
  timer.start(6000);
  time.advance(5999);
  assert.equal(expired, 0);
  time.advance(1);
  assert.equal(expired, 1);
});

test('hover and focus pause independently and resume only when both release', () => {
  const time = clock();
  let expired = 0;
  const timer = new ArrivalTimer(() => expired++, time);
  timer.start(6000);
  time.advance(2000);
  timer.pause('hover');
  timer.pause('focus');
  time.advance(10000);
  timer.resume('hover');
  time.advance(10000);
  assert.equal(expired, 0);
  timer.resume('focus');
  time.advance(3999);
  assert.equal(expired, 0);
  time.advance(1);
  assert.equal(expired, 1);
});

test('replacement cancels the prior deadline and starts a complete lifetime', () => {
  const time = clock();
  let expired = 0;
  const timer = new ArrivalTimer(() => expired++, time);
  timer.start(6000);
  time.advance(5000);
  timer.start(6000);
  time.advance(1000);
  assert.equal(expired, 0);
  time.advance(5000);
  assert.equal(expired, 1);
});

test('cancel prevents a detached or dismissed card from expiring later', () => {
  const time = clock();
  let expired = 0;
  const timer = new ArrivalTimer(() => expired++, time);
  timer.start(6000);
  timer.cancel();
  time.advance(10000);
  assert.equal(expired, 0);
});

test('an arrival already under focus starts paused', () => {
  const time = clock();
  let expired = 0;
  const timer = new ArrivalTimer(() => expired++, time);
  timer.start(6000, ['focus']);
  time.advance(10000);
  assert.equal(expired, 0);
  timer.resume('focus');
  time.advance(6000);
  assert.equal(expired, 1);
});
