24-column page row. Slots take at most 24 columns; incomplete rows align left. SlotRow contains equal-height Blocks on the columns of its Slot. Stack remains available for legacy layouts.
```jsx
<Row><Slot span={16}><SlotRow><Block span={16} title="Решения" /></SlotRow></Slot>
  <Slot span={8}><SlotRow><Block span={8} title="Тур" /></SlotRow><SlotRow><Block span={8} title="История" /></SlotRow></Slot>
</Row>
```
