Page shell: header on top, body = vertical stack of rows (gap 12) in a 1280 container on a 1440 canvas.
```jsx
<Page header={<Header active="contests" />}>
  <Row><Block span={24} title="…" /></Row>
  <Row><Block span={16} /><Block span={8} /></Row>
</Page>
```
