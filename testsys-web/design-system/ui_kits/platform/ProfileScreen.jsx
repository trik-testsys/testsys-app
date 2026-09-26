function ProfileScreen({ go, toast }) {
  const { Page, Header, Row, Stack, Block, Avatar, Tag, Button, IconButton, Tabs, Select, DataTable, Verdict, Pagination, StatusBadge } = window.TS;
  const [tab, setTab] = React.useState('all');
  const [lang, setLang] = React.useState('all');
  const all = [
    ['48213', '24.09 14:32', 'C. Разрезание', 'Весенний кубок', 'C++ 17', 'wa', '—', '—'],
    ['48190', '24.09 14:18', 'B. Скобки', 'Весенний кубок', 'Python', 'ok', '48 мс', '9.1 МБ'],
    ['48102', '24.09 13:57', 'B. Скобки', 'Весенний кубок', 'Python', 'tle', '1000 мс', '8.8 МБ'],
    ['47966', '24.09 13:40', 'A. Сумма', 'Весенний кубок', 'C++ 17', 'ok', '15 мс', '3.4 МБ'],
    ['46511', '21.09 19:02', 'Графы на решётке', 'Архив', 'C++ 17', 'ok', '212 мс', '31 МБ'],
    ['46498', '21.09 18:47', 'Графы на решётке', 'Архив', 'C++ 17', 're', '4 мс', '2.1 МБ']
  ].map(r => ({ id: r[0], time: r[1], prob: r[2], contest: r[3], lang: r[4], v: r[5], ms: r[6], mem: r[7] }));
  const rows = all.filter(r => (tab === 'all' || (tab === 'ok' ? r.v === 'ok' : r.v !== 'ok')) && (lang === 'all' || r.lang.startsWith(lang)));
  return (
    <Page header={<Header active="rating" user={{ name: 'Анна Смирнова', short: 'Анна С.' }} notifications />}>
      <Block bodyStyle={{ padding: '28px 32px', flexDirection: 'row', alignItems: 'center', gap: 28 }}>
        <Avatar name="Анна Смирнова" size={96} tone={0} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}><h1 className="ts-h1">Анна Смирнова</h1><Tag variant="dark">★ 1842</Tag><Tag variant="up">+38</Tag></div>
          <div className="ts-muted" style={{ display: 'flex', gap: 16 }}><span className="ts-mono">@anna_s</span><span>МФТИ</span><span>Москва</span><span>На платформе с 2023</span></div>
        </div>
        <div style={{ display: 'flex', gap: 32, padding: '0 32px', borderLeft: '1px solid var(--line)', borderRight: '1px solid var(--line)' }}>
          {[['312', 'решено задач'], ['47', 'соревнований'], ['128', 'место в рейтинге']].map(s => <div key={s[1]} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}><span className="ts-mono" style={{ font: '700 24px/32px var(--font-mono)' }}>{s[0]}</span><span className="ts-muted" style={{ fontSize: 13 }}>{s[1]}</span></div>)}
        </div>
        <div style={{ display: 'flex', gap: 8 }}><Button variant="secondary" icon="pencil">Редактировать</Button><IconButton icon="ellipsis" label="Ещё" /></div>
      </Block>
      <Row>
        <Block span={16} flush title={<Tabs bare size="lg" value={tab} onChange={setTab} items={[{ value: 'all', label: 'Все посылки' }, { value: 'ok', label: 'Принятые' }, { value: 'err', label: 'С ошибками' }]} />}
          actions={<div style={{ width: 150 }}><Select value={lang} onChange={setLang} options={[{ value: 'all', label: 'Язык: все' }, { value: 'C++', label: 'C++' }, { value: 'Python', label: 'Python' }]} /></div>}
          footer={<><span className="ts-muted" style={{ flex: 1, fontSize: 13 }}>{rows.length} из 1 412</span><Pagination page={1} total={57} compact /></>}>
          <DataTable rows={rows} columns={[
            { key: 'id', title: 'ID', mono: true, render: r => <a href="#" className="ts-mono">#{r.id}</a> },
            { key: 'time', title: 'Время', mono: true, muted: true },
            { key: 'prob', title: 'Задача', render: r => <div style={{ display: 'flex', flexDirection: 'column' }}><b style={{ fontWeight: 600 }}>{r.prob}</b><span className="ts-muted" style={{ fontSize: 12 }}>{r.contest}</span></div> },
            { key: 'lang', title: 'Язык', muted: true },
            { key: 'v', title: 'Вердикт', render: r => <Verdict code={r.v} /> },
            { key: 'ms', title: 'Время', mono: true, align: 'right' },
            { key: 'mem', title: 'Память', mono: true, align: 'right' }
          ]} />
        </Block>
        <Stack span={8}>
          <Block title="Предстоящие" actions={<a href="#" style={{ fontSize: 13, fontWeight: 600 }}>Все</a>} flush>
            {[['Весенний кубок 2026', 'Зарегистрирована', 'info', 'Регистрация', '26.09 10:00'], ['Квиз по алгоритмам', 'Можно присоединиться', 'live', 'Идёт', 'до 18:00']].map(u => (
              <div key={u[0]} className="ts-list-row" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}><b style={{ fontWeight: 600 }}>{u[0]}</b><StatusBadge tone={u[2]} size="sm">{u[3]}</StatusBadge></div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}><span className="ts-muted">{u[1]}</span><span className="ts-mono" style={{ fontWeight: 600 }}>{u[4]}</span></div>
              </div>
            ))}
          </Block>
          <Block title="История участия" flush>
            {[['Осенний марафон', '14.09.2026', '4 / 812', '+38'], ['Командная олимпиада', '02.09.2026', '17 / 240', '+12'], ['Кубок первокурсника', '18.08.2026', '41 / 1 105', '−9']].map(h => (
              <div key={h[0]} className="ts-list-row">
                <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}><b style={{ fontWeight: 600 }}>{h[0]}</b><span className="ts-muted" style={{ fontSize: 12 }}>{h[1]}</span></div>
                <span className="ts-mono" style={{ fontWeight: 600, fontSize: 13 }}>{h[2]}</span>
                <span className="ts-mono" style={{ fontWeight: 600, fontSize: 12, minWidth: 40, textAlign: 'right', color: h[3][0] === '−' ? 'var(--danger-fg)' : 'var(--success-fg)' }}>{h[3]}</span>
              </div>
            ))}
          </Block>
        </Stack>
      </Row>
    </Page>
  );
}
window.ProfileScreen = ProfileScreen;
