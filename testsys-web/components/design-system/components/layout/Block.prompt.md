The universal container; every piece of content on a page lives in a Block inside a Row.
```jsx
<Block span={16} title="Посылки" subtitle="24 сегодня" actions={<Button size="sm" variant="secondary">Экспорт</Button>} flush footer={<Pagination page={1} total={24} />}>
  <DataTable … />
</Block>
```
White, 1px line, radius 12. Head 8/12 padding, body 12, footer on sunken.

With `grid`, put horizontal fields inside `BlockRow`: body and rows continue the page subgrid.
