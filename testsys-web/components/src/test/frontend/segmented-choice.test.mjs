import assert from 'node:assert/strict';
import test from 'node:test';
import {segmentedChoice} from '../../main/resources/META-INF/frontend/testsys-ui/segmented-choice.ts';

function fixture(disabled = []) {
  const group = new EventTarget();
  group.disabled = false;
  group.readonly = false;
  const radios = ['a', 'b', 'c'].map(value => ({
    disabled: disabled.includes(value),
    click() { group.value = value; group.clicks++; },
    focus() { group.focused = value; }
  }));
  group.clicks = 0;
  group.querySelectorAll = () => radios;
  segmentedChoice.attach(group);
  return {group, key(key, source = radios[1]) {
    const event = new Event('keydown', {cancelable: true});
    Object.defineProperties(event, {key: {value: key}, composedPath: {value: () => [source, group]}});
    group.dispatchEvent(event);
    return event.defaultPrevented;
  }};
}

test('Home chooses and focuses the first enabled choice', () => {
  const {group, key} = fixture(['a']);
  assert.equal(key('Home'), true);
  assert.equal(group.value, 'b');
  assert.equal(group.focused, 'b');
});

test('End chooses and focuses the last enabled choice', () => {
  const {group, key} = fixture(['c']);
  assert.equal(key('End'), true);
  assert.equal(group.value, 'b');
  assert.equal(group.focused, 'b');
});

test('readonly groups ignore Home and preserve the choice', () => {
  const {group, key} = fixture();
  group.readonly = true;
  group.value = 'b';
  assert.equal(key('Home'), false);
  assert.equal(group.value, 'b');
  assert.equal(group.focused, undefined);
});

test('disabled groups ignore End and preserve the choice', () => {
  const {group, key} = fixture();
  group.disabled = true;
  group.value = 'b';
  assert.equal(key('End'), false);
  assert.equal(group.value, 'b');
  assert.equal(group.focused, undefined);
});

test('all disabled choices leave the key unhandled', () => {
  const {group, key} = fixture(['a', 'b', 'c']);
  assert.equal(key('Home'), false);
  assert.equal(group.clicks, 0);
});

test('repeated attach installs one handler and detach removes it', () => {
  const {group, key} = fixture();
  segmentedChoice.attach(group);
  key('End');
  assert.equal(group.clicks, 1);
  segmentedChoice.detach(group);
  assert.equal(key('Home'), false);
  assert.equal(group.clicks, 1);
});

test('arrows and keys outside radio choices remain native', () => {
  const {group, key} = fixture();
  assert.equal(key('ArrowLeft'), false);
  assert.equal(key('Home', group), false);
  assert.equal(group.clicks, 0);
});
