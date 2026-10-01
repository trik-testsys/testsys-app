function PrototypeHeader({ route, go, state, actor, searchQuery = '', onSearch }) {
  const { Header } = window.TS;
  const navigation = window.PrototypeNavigation;
  const user = state.users.find(user => user.id === state.sessionUserId);
  return <Header sticky compact currentTarget={route.key} brandTarget="home" items={navigation.getHeaderItems(searchQuery)} active={route.key === 'home' ? 'home' : route.cabinetKey ? 'menu' : undefined} searchMenuKey="menu" searchValue={searchQuery} onSearch={onSearch}
    user={user ? { name: user.alias } : undefined} onNavigate={go} searchPlaceholder="Найти кабинет или раздел…"
    notificationItems={[{ label: 'Демонстрационные данные сохраняются при переходах' }]}
    userMenuItems={[{ label: 'Главная', onClick: () => go('home') }, ...(user ? [{ label: 'Обзор своего кабинета', onClick: () => go(navigation.getCabinetRoute(user.role)) }] : []), { label: 'Вход', onClick: () => go('login', { authMode: 'in' }) }, { label: 'Восстановить доступ', onClick: () => go('login', { authMode: 'restore' }) }]}
    onSignIn={() => go('login', { authMode: 'in' })} onSignUp={() => go('login', { authMode: 'up' })} />;
}
function PrototypeInfoFields({ fields, columns = 24 }) {
  const { BlockRow, Field, Input, Textarea } = window.TS;
  const labelSize = columns <= 8 ? 3 : 4;
  return <>{fields.map(field => <BlockRow key={field.label}><Field label={field.label} labelSize={labelSize} size={columns - labelSize}>
    {field.multiline ? <Textarea readOnly value={field.value == null || field.value === '' ? 'Не указано' : String(field.value)} /> : <Input readOnly mono={field.mono} value={field.value == null || field.value === '' ? 'Не указано' : String(field.value)} />}
  </Field></BlockRow>)}</>;
}
function PrototypeBreadcrumbs(route, go) {
  return window.PrototypeNavigation.getBreadcrumbItems(route.key).map(item => ({ label: item.label, href: '#' + item.target, onClick: event => { event.preventDefault(); go(item.target); } }));
}
window.PrototypeHeader = PrototypeHeader;
window.PrototypeInfoFields = PrototypeInfoFields;
window.PrototypeBreadcrumbs = PrototypeBreadcrumbs;
