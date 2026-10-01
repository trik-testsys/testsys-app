// Returns original text segments, merging all overlapping literal case-insensitive matches.
export function splitSearchMatches(value, query = '') {
  const text = String(value ?? '');
  const needle = String(query ?? '').trim().toLocaleLowerCase('ru-RU');
  if (!needle) return [{ text, matched: false }];
  const normalized = text.toLocaleLowerCase('ru-RU');
  const starts = [];
  const ends = [];
  let offset = 0;
  for (const character of text) {
    const length = character.toLocaleLowerCase('ru-RU').length;
    for (let index = 0; index < length; index++) { starts.push(offset); ends.push(offset + character.length); }
    offset += character.length;
  }
  const ranges = [];
  let from = 0;
  while (from <= normalized.length - needle.length) {
    const index = normalized.indexOf(needle, from);
    if (index < 0) break;
    const start = starts[index];
    const end = ends[index + needle.length - 1];
    const previous = ranges[ranges.length - 1];
    if (previous && start <= previous.end) previous.end = Math.max(previous.end, end);
    else ranges.push({ start, end });
    from = index + 1;
  }
  if (!ranges.length) return [{ text, matched: false }];
  const parts = [];
  let cursor = 0;
  for (const range of ranges) {
    if (range.start > cursor) parts.push({ text: text.slice(cursor, range.start), matched: false });
    parts.push({ text: text.slice(range.start, range.end), matched: true });
    cursor = range.end;
  }
  if (cursor < text.length) parts.push({ text: text.slice(cursor), matched: false });
  return parts;
}
