12-column grid row. Children spans must sum to 12: 12 · 8+4 · 6+6 · 4+4+4 · 3+3+3+3 · 3+6+3 · 7+5. Also exports `Stack` to stack several blocks inside one column.
```jsx
<Row>
  <Block span={8} title="Посылки" />
  <Stack span={4}><Block title="Предстоящие" /><Block title="История" /></Stack>
</Row>
```
