function OrganizerScreen({ go, toast }) {
  const { SortableList, Header, Row, Block, StatCard, Timer, ProgressBar, Breadcrumbs, StatusBadge, Button, Tabs, FilterChip, DataTable, Verdict, Menu, Counter, FileDrop, Dialog, Field, Input, Textarea } = window.TS;
  const [tab, setTab] = React.useState('overview');
  const [vf, setVf] = React.useState('all');
  const [frozen, setFrozen] = React.useState(false);
  const [modal, setModal] = React.useState(null);
  const [answered, setAnswered] = React.useState({});
  const [probs, setProbs] = React.useState([['Сумма', 'Лёгкая'], ['Скобки', 'Лёгкая'], ['Разрезание', 'Средняя'], ['Треугольники', 'Средняя'], ['Связность', 'Сложная'], ['Отрезки', 'Сложная']].map((p, i) => ({ id: i, name: p[0], lvl: p[1] })));
  const endsAt = React.useMemo(() => Date.now() + (3600 + 24 * 60 + 37) * 1000, []);
  const subs = [['48241', '14:36:02', 'Команда «Сигма»', 'E', 'C++ 17', 'queue'], ['48240', '14:35:47', 'Илья Петров', 'E', 'C++ 17', 'wa'], ['48238', '14:35:10', 'Мария Ким', 'D', 'Java 21', 'tle'], ['48235', '14:34:31', 'Анна Смирнова', 'F', 'C++ 17', 'ok'], ['48233', '14:33:58', 'Денис Орлов', 'C', 'Python', 'ce'], ['48230', '14:33:12', 'Елена Волкова', 'B', 'Python', 'ok']]
    .map(r => ({ id: r[0], time: r[1], who: r[2], p: r[3], lang: r[4], v: r[5] }))
    .filter(r => vf === 'all' || (vf === 'ok' ? r.v === 'ok' : vf === 'queue' ? r.v === 'queue' : r.v !== 'ok' && r.v !== 'queue'));
  const qs = [[1, 'D', 'Мария Ким', '14:21', 'Может ли n быть равно нулю во входных данных?'], [2, 'E', 'Команда «Сигма»', '14:05', 'Гарантируется ли, что граф связный?']];
  const open = qs.filter(q => !answered[q[0]]).length;
  return (
    <div className="ts-app">
      <Header active="contests" user={{ name: 'Анна Смирнова', short: 'Анна С.' }} />
      <div style={{ background: 'var(--white)', borderBottom: '1px solid var(--line)' }}>
        <div style={{ maxWidth: 'var(--container)', margin: '0 auto', paddingTop: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
          <Breadcrumbs items={[{ label: 'Мои соревнования' }, { label: 'Весенний кубок 2026' }]} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <h1 className="ts-h1">Весенний кубок 2026</h1>
            <StatusBadge tone="live">Идёт</StatusBadge>
            <span className="ts-muted" style={{ fontSize: 13 }}>ICPC · 10:00–15:00</span>
            <span style={{ flex: 1 }} />
            <Button variant="secondary" icon="megaphone" onClick={() => setModal('ann')}>Объявление</Button>
            <Button variant="secondary" icon="snowflake" onClick={() => { setFrozen(!frozen); toast({ tone: 'warning', title: frozen ? 'Таблица разморожена' : 'Таблица заморожена' }); }}>{frozen ? 'Разморозить' : 'Заморозить таблицу'}</Button>
            <Button variant="danger" onClick={() => setModal('end')}>Завершить</Button>
          </div>
          <Tabs bare size="lg" value={tab} onChange={setTab} items={[{ value: 'overview', label: 'Обзор' }, { value: 'problems', label: 'Задачи' }, { value: 'people', label: 'Участники' }, { value: 'subs', label: 'Посылки' }, { value: 'q', label: 'Вопросы', count: open || undefined, countTone: 'danger' }, { value: 'settings', label: 'Настройки' }]} />
        </div>
      </div>
      {tab === 'problems' ? (
      <main className="ts-page" style={{ paddingTop: 24 }}>
        <Row>
          <Block span={16} title="Порядок задач" subtitle="Перетащите задачу за ⋮⋮, буквы обновятся автоматически" actions={<Button size="sm" variant="secondary" icon="plus">Добавить из архива</Button>}>
            <SortableList items={probs} onChange={p => { setProbs(p); toast({ tone: 'success', title: 'Порядок задач сохранён' }); }} renderItem={(p, i) => <>
              <span className="ts-filetype" style={{ width: 28, height: 28, borderRadius: 6, fontSize: 13 }}>{'ABCDEFGH'[i]}</span>
              <b style={{ flex: 1, fontWeight: 600 }}>{p.name}</b>
              <span className="ts-muted" style={{ fontSize: 13 }}>{p.lvl}</span>
              <span className="ts-mono ts-muted" style={{ fontSize: 12, minWidth: 96, textAlign: 'right' }}>1 с · 256 МБ</span>
            </>} />
          </Block>
          <Block span={8} title="Подсказка"><span className="ts-muted">Порядок влияет только на буквы и сортировку в таблице результатов. Посылки участников сохраняются за задачей.</span></Block>
        </Row>
      </main>
      ) : (
      <main className="ts-page" style={{ paddingTop: 24 }}>
        <Row align="stretch">
          <StatCard span={6} label="Участников" value="1 284" delta="+36 за час" trend="up" />
          <StatCard span={6} label="Посылок" value="9 412" delta="≈ 140 в минуту" />
          <StatCard span={6} label="В очереди" value="23" delta="Среднее ожидание 4 с" />
          <StatCard span={6} dark label="До окончания" value={<Timer to={endsAt} variant="text" />}><ProgressBar value={72} thin inverse style={{ marginTop: 8 }} /></StatCard>
        </Row>
        <Row>
          <Block span={16} flush title="Посылки в реальном времени" actions={[['all', 'Все'], ['ok', 'Принятые'], ['err', 'Ошибки'], ['queue', 'В очереди']].map(f => <FilterChip key={f[0]} selected={vf === f[0]} onChange={() => setVf(f[0])}>{f[1]}</FilterChip>)}>
            <DataTable rows={subs} columns={[
              { key: 'id', title: 'ID', mono: true, render: r => <a href="#" className="ts-mono">#{r.id}</a> },
              { key: 'time', title: 'Время', mono: true, muted: true },
              { key: 'who', title: 'Участник', render: r => <b style={{ fontWeight: 600 }}>{r.who}</b> },
              { key: 'p', title: 'Задача', mono: true },
              { key: 'lang', title: 'Язык', muted: true },
              { key: 'v', title: 'Вердикт', render: r => <Verdict code={r.v} /> },
              { key: 'm', title: '', width: 52, render: r => <Menu onSelect={a => toast({ tone: 'info', title: a, description: 'Посылка #' + r.id })} items={[{ label: 'Открыть', kbd: '↵' }, { label: 'Перепроверить', kbd: 'R' }, { separator: true }, { label: 'Дисквалифицировать', danger: true }]} /> }
            ]} />
          </Block>
          <Block span={8} flush title="Задачи" actions={<Button size="sm" variant="secondary">Изменить</Button>}>
            {[['A', 'Сумма', 1190, 1240], ['B', 'Скобки', 842, 1105], ['C', 'Разрезание', 406, 980], ['D', 'Треугольники', 188, 712], ['E', 'Связность', 64, 421], ['F', 'Отрезки', 212, 530]].map(p => (
              <div key={p[0]} className="ts-list-row">
                <span className="ts-filetype" style={{ width: 28, height: 28, fontSize: 13, borderRadius: 6 }}>{p[0]}</span>
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}><b style={{ fontWeight: 600 }}>{p[1]}</b><ProgressBar value={p[2] / p[3] * 100} tone="success" thin /></div>
                <span className="ts-mono ts-muted" style={{ fontSize: 12, minWidth: 72, textAlign: 'right' }}>{p[2]} / {p[3]}</span>
              </div>
            ))}
          </Block>
        </Row>
        <Row>
          <Block span={12} flush title="Вопросы участников" actions={open ? <Counter>{open}</Counter> : null}>
            {qs.map(q => (
              <div key={q[0]} className="ts-list-row" style={{ flexDirection: 'column', alignItems: 'stretch', gap: 10, padding: '16px 20px' }}>
                <div className="ts-muted" style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}><b className="ts-mono" style={{ color: 'var(--text-primary)' }}>{q[1]}</b>{q[2]} · <span className="ts-mono">{q[3]}</span><span style={{ flex: 1 }} /><StatusBadge size="sm" dot={false} tone={answered[q[0]] ? 'neutral' : 'danger'}>{answered[q[0]] || 'Новый'}</StatusBadge></div>
                <span>{q[4]}</span>
                {!answered[q[0]] ? <div style={{ display: 'flex', gap: 8 }}><Button size="sm" onClick={() => { setAnswered({ ...answered, [q[0]]: 'Отвечено' }); toast({ tone: 'success', title: 'Ответ отправлен', description: q[2] }); }}>Ответить</Button><Button size="sm" variant="secondary" onClick={() => setAnswered({ ...answered, [q[0]]: 'Без комментариев' })}>Без комментариев</Button></div> : null}
              </div>
            ))}
          </Block>
          <Block span={12} title="Материалы задачи D">
            <FileDrop hint=".zip · пары input / output" minHeight={120} onSelect={() => toast({ tone: 'success', title: 'Тесты загружены' })} />
            <FileDrop state="done" fileName="tests_D_v1.zip" fileType="ZIP" fileMeta="48 тестов · 2.3 МБ" minHeight={72} />
          </Block>
        </Row>
      </main>
      )}
      <Dialog open={modal === 'end'} variant="danger" title="Завершить соревнование?" onClose={() => setModal(null)} footer={<><Button variant="secondary" onClick={() => setModal(null)}>Отмена</Button><Button variant="danger" onClick={() => { setModal(null); toast({ tone: 'success', title: 'Соревнование завершено', description: 'Итоговая таблица опубликована' }); }}>Завершить</Button></>}>
        Приём решений остановится, таблица будет разморожена. Отменить действие нельзя.
      </Dialog>
      <Dialog open={modal === 'ann'} size="md" title="Новое объявление" onClose={() => setModal(null)} footer={<><span className="ts-muted" style={{ flex: 1, fontSize: 13 }}>Увидят 1 284 участника</span><Button variant="secondary" onClick={() => setModal(null)}>Отмена</Button><Button onClick={() => { setModal(null); toast({ tone: 'info', title: 'Объявление опубликовано' }); }}>Опубликовать</Button></>}>
        <Field label="Заголовок"><Input placeholder="Уточнение к задаче D" /></Field>
        <Field label="Текст"><Textarea placeholder="Текст объявления" /></Field>
      </Dialog>
    </div>
  );
}
window.OrganizerScreen = OrganizerScreen;
