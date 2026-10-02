Single-choice dropdown (language, format, timezone).
```jsx
<Select value={lang} onChange={setLang} options={[{ value: 'cpp', label: 'C++', meta: 'GCC 13' }, { value: 'py', label: 'Python', meta: '3.12' }]} />
```

Controls have accessible names, Enter/Space opening and Escape closing. MultiSelect changes immediately; Apply only closes and calls onApply.
