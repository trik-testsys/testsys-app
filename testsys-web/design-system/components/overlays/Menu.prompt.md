Row / object action menu behind "⋯". Destructive item last, after a separator.
```jsx
<Menu onSelect={act} items={[{ label: 'Открыть', kbd: '↵' }, { label: 'Перепроверить', kbd: 'R' }, { separator: true }, { label: 'Дисквалифицировать', danger: true }]} />
```

Popover returns focus on Escape. Menu disables unavailable items and supports arrow/Home/End navigation; shortcut hints are not rendered without implemented bindings.
