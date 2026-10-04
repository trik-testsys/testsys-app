Панель фильтров — контейнер для обычных BlockRow, Field и контролов формы. Начинает свёрнутой;
закрытие сохраняет черновик и ошибки, успешное применение не закрывает панель.
Страница хранит draft, applied и defaults отдельно; onApply возвращает false при ошибке,
onReset восстанавливает defaults, onRefresh возвращает таблицу на первую страницу.

```jsx
<Block grid flush filters={<TableFilters onApply={applyDraft} onReset={resetDefaults}
  onRefresh={() => setPage(1)}>
  <BlockRow><Field label="Участник" labelSize={4} size={20}><Input value={draft.name} onChange={changeName} /></Field></BlockRow>
</TableFilters>}><DataTable rows={rows} columns={columns} /></Block>
```

Работающий пример — components/data/data.card.html. Compact select и FilterChip остаются отдельными примерами.
