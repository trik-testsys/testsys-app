Table for participants, submissions, problems; lives inside a flush Block body.
```jsx
<Block title="Посылки" flush>
  <DataTable rows={subs} columns={[{ key: 'id', title: 'ID', mono: true }, { key: 'v', title: 'Вердикт', render: r => <Verdict code={r.v} /> }]} />
</Block>
```
Header row sits on sunken bg; selection row = accent-softer. Put Pagination in the Block footer.
Prefer a semantic column `width` over pixels: `'narrow'` for IDs, scores and dates, `'medium'` for names and statuses,
`'wide'` for titles, `'fill'` for the column that takes the rest of the row (`.ts-col--*`, tokens `--col-*`).
