Label + control + hint/error wrapper — wrap every form control in it.
```jsx
<Field label="Почта" error="Введите корректный адрес">
  <Input error defaultValue="anna@mail" />
</Field>
```

Use `labelSize={3} size={9}` inside a grid BlockRow for a label on the left. `controlId` links the label, `required` marks the control. Hint/error stay below the control; omit both sizes for vertical layout.

Clicking a Field label focuses composite controls such as MultiSelect and FileDrop. Checkbox exposes a native button target; read-only display values remain noninteractive.
