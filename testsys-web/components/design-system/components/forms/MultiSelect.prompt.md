Dropdown for choosing several values (topics, languages, verdict filters) with search, select-all (tri-state), chips and apply footer.
```jsx
<MultiSelect value={topics} onChange={setTopics} placeholder="Выберите темы"
  options={[{ value: 'graphs', label: 'Графы', meta: 412 }, { value: 'dp', label: 'Динамическое программирование', meta: 538 }]} />
```
Use display="count" in dense filter bars.

Controls have accessible names, Enter/Space opening and Escape closing. MultiSelect changes immediately; Apply only closes and calls onApply.
