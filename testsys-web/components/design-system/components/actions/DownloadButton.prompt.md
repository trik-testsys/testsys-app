Button for downloads that may not start instantly — controlled five-state machine (idle, preparing, downloading, done, error).
```jsx
<DownloadButton state={s} progress={p} onClick={start} onCancel={cancel} size="sm" />
```
Preparing = server is building an archive (spinner, clickable to cancel). Downloading fills the button with accent-soft. Error offers retry and should keep the reached progress. Pair with a ProgressBar + meta line ("4.1 из 12.4 МБ · 5.4 МБ/с · ~3 с") in file rows.
