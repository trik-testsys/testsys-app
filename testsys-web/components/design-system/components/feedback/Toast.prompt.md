Transient notification, bottom-right, auto-dismiss 4 s. Also exports `useToasts()` → [push, node].
```jsx
const [toast, toaster] = useToasts();
toast({ tone: 'success', title: 'Задача C принята', description: '124 мс · 8.2 МБ' });
return <>{page}{toaster}</>;
```

`useToasts` cancels pending dismissal timers when a toast closes or its host unmounts.
