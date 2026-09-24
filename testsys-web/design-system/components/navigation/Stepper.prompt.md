Horizontal progress through a multi-step flow (contest creation wizard). Put it in the Block head; Back / Next in the footer.
```jsx
<Stepper current={step} onStepClick={setStep} steps={[{ label: 'Основное', sub: 'Название и доступ' }, { label: 'Формат и время' }, { label: 'Задачи' }, { label: 'Публикация' }]} />
```
