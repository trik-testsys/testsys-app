Toggleable filter chip for list filters (Все / Идут / Регистрация); selected = cream + accent.
```jsx
<FilterChip selected={f.live} onChange={v => setF({ ...f, live: v })}>Идут</FilterChip>
<FilterChip dropdown>Язык: все</FilterChip>
```
