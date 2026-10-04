Оболочка страницы: Header, необязательный PageHead, ряды блоков и автоматический Footer.
Футтер заполняет нижний край короткой страницы и следует после содержимого длинной.

```jsx
<Page header={<Header items={[]} />} head={<PageHead title="Кабинет" />}
  footer={<Footer links={[{ label: 'Каталог компонентов', href: '../../components/index.html' }]} />}>
  <Row><Slot span={24}><SlotRow><Block span={24} title="Классы" /></SlotRow></Slot></Row>
</Page>
```

Без `footer` Page использует стандартный Footer без ссылок. Год берётся при рендере, изображение не повторяет название текстом.
