Page shell: header on top, body = vertical stack of rows (gap 24) in a 1280 container on a 1440 canvas.
```jsx
<Page header={<Header active="contests" />}>
  <Row><Block span={12} title="…" /></Row>
  <Row><Block span={8} /><Block span={4} /></Row>
</Page>
```
