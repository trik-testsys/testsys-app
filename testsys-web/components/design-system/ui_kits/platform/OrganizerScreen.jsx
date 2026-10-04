function OrganizerScreen({ go, state, dispatch, route, actor: user, selection, updateSelection, searchQuery, onSearch }) {
  const { Page, PageHead, Block, BlockRow, Field, Input, Select, Button, Verdict, Alert, DownloadButton, EmptyState, Dialog, StatusBadge } = window.TS;
  const competitions = state.competitions.filter(item => item.organizerId === user.id);
  const competition = competitions.find(item => item.id === selection.competitionId) || competitions[0] || { id: '', name: '', participantIds: [], tourIds: [] };
  const setCompetitionId = competitionId => updateSelection({ competitionId, tourId: null });
  const setTourId = tourId => updateSelection({ tourId });
  const tab = route.sectionKey === 'competitions' ? 'overview' : route.sectionKey;
  const setTab = tab => go('organizer.' + (tab === 'overview' ? 'competitions' : tab));
  const [form, setForm] = React.useState(null);
  const [errors, setErrors] = React.useState({});
  const [materials, setMaterials] = React.useState(null);
  const [exported, setExported] = React.useState('');
  const tours = state.tours.filter(item => competition.tourIds.includes(item.id));
  const tour = tours.find(item => item.id === selection.tourId) || tours[0];
  const tasks = state.tasks.filter(item => tour?.taskIds.includes(item.id));
  const participants = state.users.filter(item => competition.participantIds.includes(item.id));
  const availableTours = window.DemoModel.getOrganizerAvailableTours(state, user).filter(item => !competition.tourIds.includes(item.id));
  React.useEffect(() => { setForm(null); setErrors({}); setMaterials(null); }, [route.key, competition.id, user.id]);
  const open = type => { setErrors({}); setForm({ type, name: '', count: '3', tourId: availableTours[0]?.id || '' }); };
  const change = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const close = () => { setForm(null); setErrors({}); };
  const save = () => {
    let action;
    if (form.type === 'competition') {
      if (!form.name.trim()) { setErrors({ name: 'Введите название соревнования' }); return; }
      action = { type: 'createCompetition', name: form.name };
    } else if (form.type === 'participants') {
      const count = Number(form.count);
      if (!Number.isInteger(count) || count < 1 || count > window.DemoModel.DEMO_PARTICIPANT_LIMIT) { setErrors({ count: 'Укажите целое число от 1 до 100; это предел демонстрации' }); return; }
      action = { type: 'createParticipants', competitionId: competition.id, count };
    } else {
      if (!availableTours.some(item => item.id === form.tourId)) { setErrors({ tour: 'Выберите доступный тур' }); return; }
      action = { type: 'addTour', competitionId: competition.id, tourId: form.tourId };
    }
    const preview = window.DemoModel.reduceDemoState(state, action);
    if (preview.message?.tone === 'danger') { setErrors({ form: preview.message.text }); return; }
    dispatch(action);
    if (form.type === 'competition') setCompetitionId(preview.competitions.at(-1).id);
    close();
  };
  const csv = () => { window.DemoModel.downloadDemoFile('results-' + tour.id + '.csv', window.DemoModel.buildResultsCsv(state, competition.id, tour.id), 'text/csv;charset=utf-8'); setExported('Подготовлена полная матрица: results-' + tour.id + '.csv'); };
  const resultColumns = [{ key: 'id', title: 'ID', width: 'narrow', mono: true }, { key: 'alias', title: 'Псевдоним', width: 'medium' }, ...tasks.map(task => ({ key: task.id, title: task.id + ' · ' + task.name, width: 'wide', render: participant => { const best = window.DemoModel.getBestScore(state.solutions, participant.id, task.id); const count = state.solutions.filter(item => item.userId === participant.id && item.taskId === task.id).length; return <div className="ts-vstack">{best == null ? <span>Нет результата</span> : <Verdict score={best} label="баллов" />}<span className="ts-mono ts-muted">Решений: {count}</span></div>; } }))];
  return <Page header={<PrototypeHeader searchQuery={searchQuery} onSearch={onSearch} route={route} go={go} state={state} actor={user} />} head={<PageHead title={route.title} breadcrumbs={PrototypeBreadcrumbs(route, go)} badges={<StatusBadge tone="info">{user.role}</StatusBadge>} />}>
    <PrototypeTable title="Соревнования организатора" rows={competitions} contextKey={user.id + ':competitions'} resetKey={state.nextId} actions={<Button size="sm" onClick={() => open('competition')}>Создать соревнование</Button>} columns={[{ key: 'name', title: 'Название', width: 'fill' }, { key: 'open', title: 'Действие', render: item => <Button size="sm" variant="secondary" onClick={() => setCompetitionId(item.id)}>Выбрать</Button> }]} empty={<EmptyState title="Нет собственных соревнований" description="Создайте первое соревнование действием в шапке списка." />} />
    <Block grid title="Выбранное соревнование"><BlockRow><Field label="Соревнование" labelSize={4} size={20}><Select disabled={!competitions.length} options={competitions.map(item => ({ value: item.id, label: item.name }))} value={competition.id} onChange={setCompetitionId} /></Field></BlockRow><PrototypeInfoFields fields={[{ label: 'ID соревнования', value: competition.id || 'Не выбрано', mono: true }, { label: 'Название', value: competition.name || 'Нет собственных соревнований' }]} /></Block>
    {state.message ? <Block><Alert tone={state.message.tone} title={state.message.text} /></Block> : null}
    {tab === 'overview' ? <Block grid title="Сведения"><PrototypeInfoFields fields={[{ label: 'Организатор', value: user.alias }, { label: 'Участников', value: participants.length, mono: true }, { label: 'Туров', value: tours.length, mono: true }]} /><BlockRow><div className="ts-hstack" style={{ gridColumn: '1 / -1' }}><Button onClick={() => setTab('participants')}>Открыть участников</Button><Button variant="secondary" onClick={() => setTab('results')}>Открыть результаты</Button></div></BlockRow></Block> : null}
    {tab === 'participants' ? <PrototypeTable title="Участники" rows={participants} contextKey={user.id + ':' + competition.id + ':participants'} resetKey={state.nextId} actions={<Button size="sm" disabled={!competition.id} onClick={() => open('participants')}>Создать участников</Button>} columns={[{ key: 'id', title: 'ID', width: 'narrow', mono: true }, { key: 'alias', title: 'Псевдоним', width: 'fill' }, { key: 'lastLogin', title: 'Последний вход', width: 'medium', mono: true }, { key: 'accessCode', title: 'Код-доступа', mono: true }]} empty={<EmptyState title="Участников пока нет" description="Создайте участников с кодами-доступа." action={<Button disabled={!competition.id} onClick={() => open('participants')}>Создать участников</Button>} />} /> : null}
    {tab === 'tours' ? <PrototypeTable title="Туры соревнования" rows={tours} contextKey={user.id + ':' + competition.id + ':tours'} actions={<Button size="sm" disabled={!competition.id || !availableTours.length} onClick={() => open('tour')}>Добавить тур</Button>} dateValue={item => item.startsAt} columns={[{ key: 'name', title: 'Название', width: 'fill' }, { key: 'startsAt', title: 'Начало', width: 'medium', mono: true }, { key: 'endsAt', title: 'Конец', width: 'medium', mono: true }, { key: 'durationMinutes', title: 'Минут', mono: true }, { key: 'open', title: 'Действие', render: item => <Button size="sm" variant="secondary" onClick={() => { setTourId(item.id); setTab('results'); }}>Результаты тура</Button> }]} empty={<EmptyState title="Туров пока нет" description={availableTours.length ? 'Добавьте доступный существующий тур.' : 'Доступных туров для добавления нет.'} />} /> : null}
    {tab === 'results' ? tour ? <><Block grid title="Тур и материалы"><BlockRow><Field label="Тур" labelSize={4} size={20}><Select value={tour.id} onChange={setTourId} options={tours.map(item => ({ value: item.id, label: item.name }))} /></Field></BlockRow><PrototypeInfoFields fields={[{ label: 'Название тура', value: tour.name }, { label: 'Начало', value: tour.startsAt, mono: true }, { label: 'Конец', value: tour.endsAt, mono: true }, { label: 'Длительность', value: tour.durationMinutes + ' минут', mono: true }]} /><BlockRow><div className="ts-vstack" style={{ gridColumn: '1 / -1' }}><p>{tour.description}</p><div className="ts-hstack">{tasks.map(item => <Button key={item.id} variant="secondary" onClick={() => setMaterials(item)}>Материалы: {item.name}</Button>)}</div></div></BlockRow></Block><PrototypeTable title="Участник × задача" subtitle="Лучший балл и количество решений; CSV содержит полную матрицу" rows={participants} columns={resultColumns} contextKey={user.id + ':' + competition.id + ':' + tour.id + ':results'} searchText={item => item.id + ' ' + item.alias} actions={<DownloadButton labels={{ idle: 'Скачать CSV' }} onClick={csv} />} empty={<EmptyState title="Нет участников" description="Откройте список участников для создания." action={<Button onClick={() => setTab('participants')}>Открыть участников</Button>} />}><p role="status" style={{ padding: 12 }}>{exported}</p></PrototypeTable></> : <Block><EmptyState title="Нет доступных туров" description="Добавьте существующий тур для просмотра результатов." action={<Button onClick={() => setTab('tours')}>Открыть туры</Button>} /></Block> : null}
    <Dialog open={!!form} size="md" title={form?.type === 'competition' ? 'Создать соревнование' : form?.type === 'participants' ? 'Создать участников' : 'Добавить доступный тур'} onClose={close} footer={<><Button variant="secondary" onClick={close}>Отмена</Button><Button onClick={save}>{form?.type === 'tour' ? 'Добавить' : 'Создать'}</Button></>}>
      <div className="ts-dialog__grid">
        {form?.type === 'competition' ? <BlockRow><Field label="Название" labelSize={4} size={8} required error={errors.name}><Input value={form.name} error={!!errors.name} onChange={event => change('name', event.target.value)} /></Field></BlockRow> : null}
        {form?.type === 'participants' ? <><PrototypeInfoFields columns={12} fields={[{ label: 'Соревнование', value: competition.name }]} /><BlockRow><Field label="Количество" labelSize={4} size={8} required error={errors.count} hint="По умолчанию 3; от 1 до 100 за вызов только в демонстрации"><Input type="number" min={1} max={window.DemoModel.DEMO_PARTICIPANT_LIMIT} step={1} value={form.count} error={!!errors.count} onChange={event => change('count', event.target.value)} /></Field></BlockRow></> : null}
        {form?.type === 'tour' ? <BlockRow><Field label="Доступный тур" labelSize={4} size={8} required error={errors.tour}><Select value={form.tourId} options={availableTours.map(item => ({ value: item.id, label: item.name }))} onChange={value => change('tourId', value)} /></Field></BlockRow> : null}
      </div>
      {errors.form ? <Alert tone="danger" title={errors.form} /> : null}
    </Dialog>
    <Dialog open={!!materials} title={materials?.name} onClose={() => setMaterials(null)} footer={<Button variant="secondary" onClick={() => setMaterials(null)}>Закрыть</Button>}><p>{materials?.description}</p><p>Просмотр материалов · редактирование доступно разработчику</p><div className="ts-vstack">{materials ? [{ category: 'Условие', name: materials.id + '-statement.txt' }, { category: 'Полигон', name: materials.id + '-polygon-demo.xml' }, { category: 'Упражнение', name: materials.id + '-exercise-demo.qrs' }, ...materials.authorSolutionKinds.map(kind => ({ category: 'Авторское решение · ' + window.DemoModel.solutionKindLabels[kind], name: materials.id + '-author-demo.' + (kind === 'Python' ? 'py' : kind === 'JavaScript' ? 'js' : 'qrs') }))].map(resource => <div key={resource.name} className="ts-hstack"><span style={{ flex: 1 }}>{resource.category} · <span className="ts-mono">{resource.name}</span></span><DownloadButton size="sm" labels={{ idle: 'Скачать пример' }} onClick={() => { window.DemoModel.downloadDemoFile(resource.name, 'TestSys demo resource: ' + resource.category + '\n' + materials.description); setExported('Подготовлен пример: ' + resource.name); }} /></div>) : null}</div><p role="status">{exported}</p><a href="../../components/forms/forms.card.html">Отдельный пример редактора ресурсов для разработчика</a></Dialog>
  </Page>;
}
window.OrganizerScreen = OrganizerScreen;
