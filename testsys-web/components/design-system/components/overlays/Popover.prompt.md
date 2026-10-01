Floating panel anchored to a trigger (profile card, calendar, time list). Closes on outside click.
```jsx
<Popover trigger={<Button variant="link">Анна Смирнова</Button>} width={300}>…</Popover>
```

Popover returns focus on Escape. Menu disables unavailable items and supports arrow/Home/End navigation; shortcut hints are not rendered without implemented bindings.
