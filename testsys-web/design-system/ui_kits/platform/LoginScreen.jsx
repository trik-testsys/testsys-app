function LoginScreen({ go, toast }) {
  const { Page, Header, Row, Block, StatusBadge, SegmentedControl, Field, Input, Checkbox, Button } = window.TS;
  const [mode, setMode] = React.useState('in');
  const [remember, setRemember] = React.useState(true);
  const upcoming = [
    ['Весенний кубок 2026', 'ICPC · 5 часов · команды до 3 человек', 'info', 'Регистрация', 'через 2 д'],
    ['Квиз по алгоритмам', '30 вопросов · 40 минут', 'live', 'Идёт', 'до 18:00'],
    ['Школьная олимпиада · 2 тур', 'IOI · 4 задачи · подзадачи', 'neutral', 'Скоро', '3 окт']
  ];
  return (
    <Page header={<Header active="contests" onSignIn={() => setMode('in')} onSignUp={() => setMode('up')} />}>
      <Row align="stretch">
        <Block span={7} dark bodyStyle={{ padding: 40, gap: 28 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <span className="ts-eyebrow" style={{ color: 'var(--ink-400)' }}>Скоро на платформе</span>
            <h1 className="ts-display" style={{ maxWidth: 520 }}>Соревнования, олимпиады и квизы в одном аккаунте</h1>
          </div>
          <div>
            {upcoming.map(u => (
              <div key={u[0]} style={{ display: 'grid', gridTemplateColumns: 'minmax(0,1fr) auto auto', gap: 20, alignItems: 'center', padding: '16px 0', borderTop: '1px solid rgba(255,255,255,.12)' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}><span style={{ font: '600 16px var(--font-sans)' }}>{u[0]}</span><span style={{ fontSize: 13, color: 'var(--ink-400)' }}>{u[1]}</span></div>
                <StatusBadge tone={u[2]} size="sm">{u[3]}</StatusBadge>
                <span className="ts-mono" style={{ fontWeight: 600, minWidth: 96, textAlign: 'right' }}>{u[4]}</span>
              </div>
            ))}
          </div>
        </Block>
        <Block span={5} bodyStyle={{ padding: 32, gap: 20 }}>
          <SegmentedControl block size="lg" value={mode} onChange={setMode} options={[{ value: 'in', label: 'Вход' }, { value: 'up', label: 'Регистрация' }]} />
          {mode === 'in' ? (
            <>
              <Field label="Почта или логин"><Input size="lg" placeholder="anna@example.com" /></Field>
              <Field label="Пароль" aside={<a href="#" style={{ fontSize: 13 }}>Забыли пароль?</a>}><Input size="lg" type="password" defaultValue="password" /></Field>
              <Checkbox checked={remember} onChange={setRemember} label="Запомнить меня" />
              <Button size="lg" block onClick={() => { toast({ tone: 'success', title: 'Вы вошли', description: 'Добро пожаловать, Анна' }); go('profile'); }}>Войти</Button>
            </>
          ) : (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <Field label="Имя"><Input size="lg" placeholder="Анна" /></Field>
                <Field label="Фамилия"><Input size="lg" placeholder="Смирнова" /></Field>
              </div>
              <Field label="Почта" error="Введите корректный адрес почты"><Input size="lg" error defaultValue="anna@mail" /></Field>
              <Field label="Пароль" hint="Средний пароль · добавьте цифры и символы"><Input size="lg" type="password" defaultValue="password12" /></Field>
              <Checkbox checked label={<span className="ts-muted" style={{ fontSize: 13 }}>Принимаю <a href="#">условия использования</a></span>} />
              <Button size="lg" block onClick={() => toast({ tone: 'info', title: 'Письмо отправлено', description: 'Подтвердите почту, чтобы продолжить' })}>Создать аккаунт</Button>
            </>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: 13, color: 'var(--text-tertiary)' }}><span style={{ flex: 1, height: 1, background: 'var(--line)' }} />или<span style={{ flex: 1, height: 1, background: 'var(--line)' }} /></div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}><Button variant="secondary" size="lg">GitHub</Button><Button variant="secondary" size="lg">Яндекс ID</Button></div>
        </Block>
      </Row>
    </Page>
  );
}
window.LoginScreen = LoginScreen;
