import React from 'react';
import { Icon } from '../core/Icon.jsx';
import { ProgressBar } from '../display/ProgressBar.jsx';
const cx = (...a) => a.filter(Boolean).join(' ');

export function FileDrop({ state = 'empty', title = 'Перетащите файл', hint = 'или выберите · до 256 КБ', fileName, fileType, fileMeta, progress = 0, error, accept, multiple = false, onSelect, minHeight = 150, className }) {
  const [drag, setDrag] = React.useState(false);
  const input = React.useRef(null);
  const st = drag ? 'drag' : state;
  if (st === 'uploading' || st === 'done') {
    return (
      <div className={cx('ts-drop ts-drop--file', className)} style={{ minHeight }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="ts-filetype">{fileType || 'FILE'}</span>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', textAlign: 'left' }}>
            <span className="ts-drop__title">{fileName}</span>
            <span className="ts-hint">{fileMeta}</span>
          </div>
          {st === 'done' ? <Icon name="circle-check" size={18} style={{ color: 'var(--success)' }} /> : null}
        </div>
        {st === 'uploading' ? <ProgressBar value={progress} /> : null}
      </div>
    );
  }
  const pick = files => { setDrag(false); if (files && files.length && onSelect) onSelect(Array.from(files)); };
  return (
    <div className={cx('ts-drop', st === 'drag' && 'ts-drop--drag', st === 'error' && 'ts-drop--error', className)} style={{ minHeight }}
      onClick={() => input.current && input.current.click()}
      onDragOver={e => { e.preventDefault(); setDrag(true); }} onDragLeave={() => setDrag(false)}
      onDrop={e => { e.preventDefault(); pick(e.dataTransfer.files); }}>
      <input ref={input} type="file" hidden accept={accept} multiple={multiple} onChange={e => pick(e.target.files)} />
      {st === 'error' ? (
        <><span className="ts-drop__title">{error || 'Не удалось загрузить файл'}</span><span className="ts-hint" style={{ color: 'inherit' }}>{hint}</span></>
      ) : (
        <><Icon name="upload" size={22} strokeWidth={1.8} /><span className="ts-drop__title">{st === 'drag' ? 'Отпустите, чтобы загрузить' : title}</span>{st !== 'drag' ? <span className="ts-hint">{hint}</span> : null}</>
      )}
    </div>
  );
}
