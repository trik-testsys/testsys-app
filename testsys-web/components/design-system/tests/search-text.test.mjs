import { test } from 'node:test';
import assert from 'node:assert/strict';
import { splitSearchMatches } from '../lib/search-text.mjs';

test('should mark only the matching prefix within a word', () => {
  assert.deepEqual(splitSearchMatches('Профиль', 'про'), [{ text: 'Про', matched: true }, { text: 'филь', matched: false }]);
});
test('should preserve the text with an empty or whitespace query', () => {
  assert.deepEqual(splitSearchMatches('Профиль', '  '), [{ text: 'Профиль', matched: false }]);
});
test('should match Cyrillic case insensitively and trim the query', () => {
  assert.deepEqual(splitSearchMatches('Ресурсы и РЕСУРСЫ', '  РеСуРсЫ '), [{ text: 'Ресурсы', matched: true }, { text: ' и ', matched: false }, { text: 'РЕСУРСЫ', matched: true }]);
});
test('should merge overlapping matches while preserving the original text', () => {
  assert.deepEqual(splitSearchMatches('banana', 'ana'), [{ text: 'b', matched: false }, { text: 'anana', matched: true }]);
});
test('should match regex metacharacters as literal text', () => {
  assert.deepEqual(splitSearchMatches('x [a.*] y [a.*]', '[a.*]'), [{ text: 'x ', matched: false }, { text: '[a.*]', matched: true }, { text: ' y ', matched: false }, { text: '[a.*]', matched: true }]);
});
test('should leave titles unchanged when only a hidden alias matched filtering', () => {
  assert.deepEqual(splitSearchMatches('Ученик', 'кабинет'), [{ text: 'Ученик', matched: false }]);
});
test('should return markup-looking content as original text segments', () => {
  assert.deepEqual(splitSearchMatches('<img src=x onerror=alert(1)>', '<img'), [{ text: '<img', matched: true }, { text: ' src=x onerror=alert(1)>', matched: false }]);
});
test('should preserve Unicode offsets when lowercase expands a code point', () => {
  assert.deepEqual(splitSearchMatches('İ ПРОФИЛЬ', 'про'), [{ text: 'İ ', matched: false }, { text: 'ПРО', matched: true }, { text: 'ФИЛЬ', matched: false }]);
  assert.deepEqual(splitSearchMatches('İstanbul', 'i'), [{ text: 'İ', matched: true }, { text: 'stanbul', matched: false }]);
});
test('should preserve supplementary characters and find later matches', () => {
  assert.deepEqual(splitSearchMatches('🙂Профиль🙂ПРОФИЛЬ', 'про'), [{ text: '🙂', matched: false }, { text: 'Про', matched: true }, { text: 'филь🙂', matched: false }, { text: 'ПРО', matched: true }, { text: 'ФИЛЬ', matched: false }]);
});
