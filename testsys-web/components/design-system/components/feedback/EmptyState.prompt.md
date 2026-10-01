Placeholder for empty lists and failed loads, centred inside a Block.
```jsx
<EmptyState title="Посылок пока нет" description="Отправьте решение любой задачи." action={<Button size="sm">К задачам</Button>} />
<EmptyState error title="Не удалось загрузить" action={<Button size="sm" variant="secondary">Повторить</Button>} />
```
