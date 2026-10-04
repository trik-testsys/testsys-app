function PrototypeTable(props) {
  return <PrototypeTableContent key={(props.contextKey || props.title) + ':' + (props.resetKey ?? '')} {...props} />;
}
function PrototypeTableContent({ title, subtitle, rows, columns, rowKey = 'id', actions, empty, contextKey, resetKey, span, columnsCount = 24, queryLabel = 'Поиск', searchText, categoryValue, categoryLabel = 'Состояние', categoryOptions, dateValue, children }) {
  const { Block, BlockRow, Field, Input, Select, TableFilters, DataTable, Pagination, EmptyState, Button } = window.TS;
  const model = window.PrototypeTables;
  const defaults = { query: '', category: 'all', from: '', to: '' };
  const [state, setState] = React.useState(() => model.createTableState(defaults));
  const search = searchText || (row => columns.map(column => row[column.key]).filter(value => ['string', 'number'].includes(typeof value)).join(' '));
  const filtered = model.filterTableRows(rows, state.applied, { searchText: search, categoryValue, dateValue });
  const page = model.paginateTableRows(filtered, state.page, 10);
  const tableEmpty = rows.length > 0 && filtered.length === 0 ? <EmptyState title="Ничего не найдено" description="Измените или сбросьте фильтры, чтобы увидеть записи." action={<Button onClick={() => setState(model.createTableState(defaults))}>Сбросить фильтры</Button>} /> : empty;
  const labelSize = Math.min(4, columnsCount - 1);
  const change = (name, value) => setState(current => model.updateTableState(current, { type: 'draft', name, value }, defaults));
  const options = categoryOptions || (categoryValue ? [...new Set(rows.map(categoryValue).filter(Boolean))].map(value => ({ value: String(value), label: String(value) })) : []);
  const filters = <TableFilters columns={columnsCount} onApply={() => { const next = model.updateTableState(state, { type: 'apply' }, defaults); setState(next); return !Object.keys(next.errors).length; }} onReset={() => setState(model.createTableState(defaults))} onRefresh={() => setState(current => ({ ...current, page: 1 }))}>
    <BlockRow><Field label={queryLabel} labelSize={labelSize} size={columnsCount - labelSize}><Input value={state.draft.query} onChange={event => change('query', event.target.value)} /></Field></BlockRow>
    {categoryValue ? <BlockRow><Field label={categoryLabel} labelSize={labelSize} size={columnsCount - labelSize}><Select value={state.draft.category} onChange={value => change('category', value)} options={[{ value: 'all', label: 'Все' }, ...options]} /></Field></BlockRow> : null}
    {dateValue ? <BlockRow><Field label="Период" labelSize={labelSize} size={columnsCount - labelSize} error={state.errors.period}><PrototypeDateRange from={state.draft.from} to={state.draft.to} onChange={range => setState(current => ({ ...current, draft: { ...current.draft, ...range } }))} /></Field></BlockRow> : null}
  </TableFilters>;
  return <Block title={title} subtitle={subtitle} span={span} flush actions={actions} filters={filters} footer={<><span className="ts-muted" style={{ flex: 1 }}>Записей: {page.total}</span>{page.pageCount > 1 ? <Pagination page={page.page} total={page.pageCount} compact onChange={value => setState(current => model.updateTableState(current, { type: 'page', page: value }, defaults))} /> : null}</>}>
    {children}
    <div className="ts-table-scroll"><DataTable rows={page.rows} columns={columns} rowKey={rowKey} empty={tableEmpty} /></div>
  </Block>;
}
window.PrototypeTable = PrototypeTable;
