Single radio; group several in a flex row with gap 20.
```jsx
{['ICPC', 'IOI', 'Квиз'].map(f => <Radio key={f} label={f} checked={fmt === f} onChange={() => setFmt(f)} />)}
```
