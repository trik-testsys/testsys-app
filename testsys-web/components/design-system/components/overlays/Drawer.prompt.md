Right side panel for object details without leaving the page (submission, participant).
```jsx
<Drawer title="Посылка #48213" subtitle="C. Разрезание · Анна Смирнова" onClose={close} footer={<Button>Перепроверить</Button>}>…</Drawer>
```

The modal has a named dialog, initial focus, Tab containment, Escape closing and return to its opener.

Keyboard handling uses React bubbling: an open Select/MultiSelect consumes Escape before the modal, and nested modals handle only their own keyboard events.
