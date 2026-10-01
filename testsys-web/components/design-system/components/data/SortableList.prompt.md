Vertical list reordered by dragging the ⋮⋮ handle. Used for problem order in a contest; the lifted row follows the cursor with a soft shadow, a dashed cream slot marks the drop position, the dropped row flashes.
```jsx
<SortableList items={problems} onChange={setProblems} getKey={p => p.id}
  renderItem={(p, i) => <>
    <span className="ts-filetype" style={{ width: 28, height: 28, borderRadius: 6, fontSize: 13 }}>{'ABCDEFGH'[i]}</span>
    <b style={{ flex: 1 }}>{p.name}</b>
    <span className="ts-mono ts-muted">1 с · 256 МБ</span>
  </>} />
```
Mouse/touch via pointer events; drag starts only from the handle. Letters should come from the index so they re-letter live while dragging.
