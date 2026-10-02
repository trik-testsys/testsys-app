Single-line text input; focus ring = accent 3px.
```jsx
<Input placeholder="Название соревнования" />
<Input prefix={<Icon name="clock" />} suffix="минут" mono defaultValue="300" />
```

`readOnly` retains focus and copying, with a dashed border and transparent background, including prefix/suffix. `disabled` takes priority when both are set.
