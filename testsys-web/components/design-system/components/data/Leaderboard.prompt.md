ICPC-style standings grid with colour-coded per-problem cells.
```jsx
<Block title="Положение участников" flush>
  <Leaderboard problems={['A','B','C']} rows={[{ name: 'Анна Смирнова', org: 'МФТИ', solved: 3, penalty: 212, cells: [{ state: 'first', value: '+', time: '00:09' }, { state: 'ok', value: '+1', time: '00:41' }, { state: 'fail', value: '−2' }] }]} />
</Block>
```
