Drag-and-drop upload area for solutions, tests, statements.
```jsx
<FileDrop hint=".zip · пары input / output" onSelect={files => upload(files[0])} />
<FileDrop state="uploading" fileName="solution.cpp" fileType="CPP" fileMeta="2.6 из 4.2 КБ" progress={62} />
```

The file picker is operable with Enter/Space; selecting the same file again fires onSelect.
