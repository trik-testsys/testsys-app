24-column grid row. Children spans must sum to 24: 24 · 16+8 · 12+12 · 8+8+8 · 6+6+6+6 · 6+12+6 · 14+10. Also exports `Stack` to stack several blocks inside one column.
```jsx
<Row>
  <Block span={16} title="Посылки" />
  <Stack span={8}><Block title="Предстоящие" /><Block title="История" /></Stack>
</Row>
```
