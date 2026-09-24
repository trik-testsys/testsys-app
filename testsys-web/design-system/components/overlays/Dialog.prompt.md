Modal for confirmations and short forms. One primary action on the right.
```jsx
<Dialog variant="danger" title="Завершить соревнование?" onClose={close} footer={<><Button variant="secondary" onClick={close}>Отмена</Button><Button variant="danger">Завершить</Button></>}>
  Приём решений остановится. Отменить действие нельзя.
</Dialog>
```
