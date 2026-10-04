export function createTableState(defaults) {
  return { draft: { ...defaults }, applied: { ...defaults }, page: 1, errors: {} };
}
export function updateTableState(state, action, defaults) {
  if (action.type === 'draft') return { ...state, draft: { ...state.draft, [action.name]: action.value } };
  if (action.type === 'reset') return createTableState(defaults);
  if (action.type === 'page') return { ...state, page: action.page };
  if (action.type === 'apply') {
    const invalidDate = [state.draft.from, state.draft.to].some(value => value && !isPrototypeIsoDate(value));
    const errors = invalidDate ? { period: 'Укажите реальную дату в формате ГГГГ-ММ-ДД' } : state.draft.from && state.draft.to && state.draft.from > state.draft.to ? { period: 'Конец периода раньше начала' } : {};
    return Object.keys(errors).length ? { ...state, errors } : { ...state, applied: { ...state.draft }, page: 1, errors: {} };
  }
  return state;
}
export function prototypeDate(value) {
  const text = String(value ?? '');
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
  const match = text.match(/^(\d{2})\.(\d{2})\.(\d{4})/);
  return match ? `${match[3]}-${match[2]}-${match[1]}` : '';
}
export function filterTableRows(rows, applied, { searchText, categoryValue, dateValue }) {
  const query = applied.query.trim().toLocaleLowerCase();
  return rows.filter(row => {
    const date = dateValue ? prototypeDate(dateValue(row)) : '';
    return (!query || String(searchText(row)).toLocaleLowerCase().includes(query)) &&
      (!categoryValue || applied.category === 'all' || String(categoryValue(row)) === applied.category) &&
      (!dateValue || ((!applied.from || date >= applied.from) && (!applied.to || (date !== '' && date <= applied.to))));
  });
}
export function paginateTableRows(rows, requestedPage, size) {
  const pageCount = Math.max(1, Math.ceil(rows.length / size));
  const page = Math.min(pageCount, Math.max(1, requestedPage));
  return { rows: rows.slice((page - 1) * size, page * size), page, pageCount, total: rows.length };
}

export function isPrototypeIsoDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(0);
  date.setUTCHours(12, 0, 0, 0);
  date.setUTCFullYear(year, month - 1, day);
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}
