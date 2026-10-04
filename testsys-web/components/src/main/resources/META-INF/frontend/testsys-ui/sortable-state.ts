// Only a drag belonging to the current list snapshot and enabled mode may influence rendering or commit.
export function currentDrag<T>(drag: any, items: T[], disabled: boolean, getKey: (item: T) => string) {
  return drag && !disabled && drag.items === items && items.some(item => getKey(item) === drag.key) ? drag : null;
}

export function sortableOrder<T>(items: T[], drag: any, getKey: (item: T) => string): T[] {
  if (!drag || drag.items !== items) return items;
  const source = items.findIndex(item => getKey(item) === drag.key);
  if (source < 0) return items;
  const result = [...items];
  const moved = result.splice(source, 1)[0];
  result.splice(drag.index, 0, moved);
  return result;
}
