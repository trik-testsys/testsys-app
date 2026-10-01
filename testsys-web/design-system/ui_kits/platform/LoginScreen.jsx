function LoginScreen({ go, state, dispatch, route, authMode, onAuthModeChange, searchQuery, onSearch }) {
  const { Page, Header, Row, Slot, SlotRow, Block, StatusBadge, SegmentedControl, Field, Input, Button, Select, Alert } = window.TS;
  const [mode, setMode] = React.useState(authMode || 'in');
  const [code, setCode] = React.useState('STUDENT-2026');
  const [alias, setAlias] = React.useState('');
  const [email, setEmail] = React.useState('');
  const [role, setRole] = React.useState('Ученик');
  const [confirmation, setConfirmation] = React.useState('');
  React.useEffect(() => { setMode(authMode || 'in'); }, [authMode]);
  const changeMode = next => { onAuthModeChange?.(next); setMode(next); dispatch({ type: 'clearMessage' }); };
  const user = state.users.find(u => u.id === state.sessionUserId);
  const submit = e => {
    e.preventDefault();
    if (mode === 'in') { const next = window.DemoModel.reduceDemoState(state, { type: 'login', code }); dispatch({ type: 'login', code }); if (next.message.tone === 'success') { const signedIn = next.users.find(u => u.id === next.sessionUserId); const destination = window.DemoModel.getCabinetScreen(signedIn.role); if (destination) go(destination); } }
    if (mode === 'up') dispatch({ type: state.pendingRegistration ? 'confirmRegistration' : 'register', alias, email, role, code: confirmation });
    if (mode === 'restore') dispatch({ type: 'requestRecovery', email });
  };
  return <Page header={<PrototypeHeader searchQuery={searchQuery} onSearch={onSearch} route={route} go={go} state={state} />}>
    <Row><Slot span={14}><SlotRow><Block span={14} dark title="Решения для TRIK Studio" bodyStyle={{ padding: 32 }}>
      <h1 className="ts-display">Задачи и туры в TestSys</h1><p>Кабинет ученика показывает классы, доступные туры и собственные решения. Организатор управляет своим соревнованием и просматривает результаты.</p>
      {state.tours.map(tour => <div key={tour.id} className="ts-list-row"><div className="ts-vstack" style={{ flex: 1 }}><b>{tour.name}</b><span className="ts-mono">{tour.startsAt} — {tour.endsAt}</span><span>{tour.durationMinutes} минут · TRIK Studio {tour.trikVersion}</span></div><StatusBadge tone="info">Демонстрация</StatusBadge></div>)}
      <p>Все коды и письма ниже демонстрационные. Данные сохраняются при переключении экранов и сбрасываются верхней кнопкой.</p>
    </Block></SlotRow></Slot><Slot span={10}><SlotRow><Block span={10} title={mode === 'restore' ? 'Восстановление доступа' : 'Доступ в кабинет'} bodyStyle={{ padding: 24 }}>
      <SegmentedControl block value={mode} onChange={changeMode} options={[{ value: 'in', label: 'Вход' }, { value: 'up', label: 'Регистрация' }, { value: 'restore', label: 'Восстановить' }]} />
      <form className="ts-vstack" onSubmit={submit}>
        {mode === 'in' ? <><Field label="Код-доступа" required hint="Пример: STUDENT-2026 или ORG-2026"><Input mono value={code} onChange={e => setCode(e.target.value)} /></Field><Button type="submit" block>Войти</Button></> : mode === 'up' ? state.pendingRegistration ? <>
          <Alert tone="info" title="Демонстрационное письмо"><Field label="Получатель письма"><Input readOnly value={state.pendingRegistration.email} /></Field><Field label="Код из демонстрационного письма"><Input readOnly mono value={state.pendingRegistration.confirmationCode} /></Field></Alert>
          <Field label="Код подтверждения" required><Input mono value={confirmation} onChange={e => setConfirmation(e.target.value)} /></Field><Button type="submit">Подтвердить почту</Button>
        </> : <><Field label="Псевдоним" required><Input value={alias} onChange={e => setAlias(e.target.value)} /></Field><Field label="Почта" required><Input type="email" value={email} onChange={e => setEmail(e.target.value)} /></Field><Field label="Роль" required><Select value={role} onChange={setRole} options={['Ученик', 'Организатор']} /></Field><Button type="submit">Получить код подтверждения</Button></> : <><Field label="Привязанная почта" required hint="Пример: anna@example.com"><Input type="email" value={email} onChange={e => setEmail(e.target.value)} /></Field><Button type="submit">Подготовить письмо</Button></>}
      </form>
      {mode === 'restore' && state.recovery ? <Alert tone="info" title="Демонстрационное письмо">{state.recovery.completed ? <Field label="Новый код-доступа"><Input readOnly mono value={state.recovery.accessCode} /></Field> : <><p>Ссылка на восстановление доступа:</p><a href={'#' + state.recovery.token} onClick={e => { e.preventDefault(); dispatch({ type: 'restoreAccess', token: state.recovery.token }); }}>Получить новый код-доступа</a></>}</Alert> : null}
      {state.message ? <Alert tone={state.message.tone} title={state.message.text} /> : null}
      {user && state.message?.tone === 'success' && mode !== 'restore' ? <><Field label="Псевдоним"><Input readOnly value={user.alias} /></Field><Field label="Роль"><Input readOnly value={user.role} /></Field><Field label="Код-доступа"><Input readOnly mono value={user.accessCode} /></Field>{window.DemoModel.getCabinetScreen(user.role) ? <Button onClick={() => go(window.DemoModel.getCabinetScreen(user.role))}>Открыть кабинет</Button> : null}</> : null}
    </Block></SlotRow></Slot></Row>
  </Page>;
}
window.LoginScreen = LoginScreen;
