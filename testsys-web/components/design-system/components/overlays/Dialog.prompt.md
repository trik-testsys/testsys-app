Modal for confirmations and short forms. One primary action on the right.
```jsx
<Dialog variant="danger" title="Завершить соревнование?" onClose={close} footer={<><Button variant="secondary" onClick={close}>Отмена</Button><Button variant="danger">Завершить</Button></>}>
  Приём решений остановится. Отменить действие нельзя.
</Dialog>
```

The modal has a named dialog, initial focus, Tab containment, Escape closing and return to its opener.

Keyboard handling uses React bubbling: an open Select/MultiSelect consumes Escape before the modal, and nested modals handle only their own keyboard events.
