function HomeScreen({ route, go, state, searchQuery, onSearch }) {
  const { Page, PageHead, Row, Slot, SlotRow, Block, Button } = window.TS;
  const cabinets = window.PrototypeNavigation.cabinetDefinitions;
  return <Page header={<PrototypeHeader searchQuery={searchQuery} onSearch={onSearch} route={route} go={go} state={state} />} head={<PageHead title="Главная" />}>
    <Row><Slot span={24}><SlotRow><Block span={24} title="TestSys"><p>Система проверки решений для TRIK Studio. Задачи объединяются в туры; ученики работают в классах, участники — в соревнованиях.</p><p>Выберите кабинет для просмотра его назначения и разделов. Навигация позволяет исследовать все роли на демонстрационных данных.</p><Button onClick={() => go('login')}>Войти по коду-доступа</Button></Block></SlotRow></Slot></Row>
    {[0, 4].map(offset => <Row key={offset}>{cabinets.slice(offset, offset + 4).map(cabinet => <Slot span={6} key={cabinet.key}><SlotRow><Block span={6} title={cabinet.role} footer={<Button block onClick={() => go(window.PrototypeNavigation.getCabinetRoute(cabinet.key))}>Открыть кабинет</Button>}><p>{cabinet.purpose}</p></Block></SlotRow></Slot>)}</Row>)}
  </Page>;
}
window.HomeScreen = HomeScreen;
