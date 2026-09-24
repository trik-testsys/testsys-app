Row / object action menu behind "⋯". Destructive item last, after a separator.
```jsx
<Menu onSelect={act} items={[{ label: 'Открыть', kbd: '↵' }, { label: 'Перепроверить', kbd: 'R' }, { separator: true }, { label: 'Дисквалифицировать', danger: true }]} />
```
