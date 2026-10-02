Page shell: Header, optional PageHead and rows of slots in a 1280 container on a 1440 canvas.
```jsx
<Page header={<Header items={[]} />} head={<PageHead title="Кабинет" />}>
  <Row><Slot span={24}><SlotRow><Block span={24} title="Классы" /></SlotRow></Slot></Row>
</Page>
```
