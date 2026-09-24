Answer card for quiz questions; lay out 2 per row.
```jsx
<div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
  {opts.map((o, i) => <QuizOption key={i} letter={'ABCD'[i]} label={o} selected={sel === i} onClick={() => setSel(i)} />)}
</div>
```
