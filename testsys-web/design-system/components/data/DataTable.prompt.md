Table for participants, submissions, problems; lives inside a flush Block body.
```jsx
<Block title="Посылки" flush>
  <DataTable rows={subs} columns={[{ key: 'id', title: 'ID', mono: true }, { key: 'v', title: 'Вердикт', render: r => <Verdict code={r.v} /> }]} />
</Block>
```
Header row sits on sunken bg; selection row = accent-softer. Put Pagination in the Block footer.
