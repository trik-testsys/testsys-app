Page title strip between Header and main content; one h1, optional breadcrumbs, badges, metadata, actions and tabs.
```jsx
<Page head={<PageHead title="Мой профиль" breadcrumbs={[{ label: 'Кабинет' }]} meta="Ученик" />}>
  <Row><Slot span={24}><SlotRow><Block span={24} title="Классы" /></SlotRow></Slot></Row>
</Page>
```
