// One configuration owns all prototype routes and their presentation labels.
export const cabinetDefinitions = [
  { key: 'student', role: 'Ученик', label: 'Кабинет ученика', purpose: 'Участие в доступных турах своих классов и просмотр собственных решений.', sections: [['overview', 'Обзор'], ['profile', 'Профиль'], ['study', 'Классы и туры'], ['solutions', 'Решения']] },
  { key: 'organizer', role: 'Организатор', label: 'Кабинет организатора', purpose: 'Собственные соревнования, участники, доступные туры и результаты.', sections: [['overview', 'Обзор'], ['competitions', 'Соревнования'], ['participants', 'Участники'], ['tours', 'Туры'], ['results', 'Результаты']] },
  { key: 'developer', role: 'Разработчик', label: 'Кабинет разработчика', purpose: 'Доступные ресурсы, задачи и туры для TRIK Studio.', sections: [['overview', 'Обзор'], ['resources', 'Ресурсы'], ['tasks', 'Задачи'], ['tours', 'Туры']] },
  { key: 'judge', role: 'Судья', label: 'Кабинет судьи', purpose: 'Просмотр решений учеников и участников, результатов и материалов проверки.', sections: [['overview', 'Обзор'], ['solutions', 'Решения'], ['result', 'Результат проверки']] },
  { key: 'administrator', role: 'Администратор', label: 'Кабинет администратора', purpose: 'Пользователи своего сообщества и сведения о них.', sections: [['overview', 'Обзор'], ['users', 'Пользователи'], ['user', 'Сведения о пользователе']] },
  { key: 'participant', role: 'Участник', label: 'Кабинет участника', purpose: 'Доступные туры своего соревнования, задачи и собственные решения.', sections: [['overview', 'Обзор'], ['tours', 'Доступные туры'], ['solutions', 'Задачи и решения']] },
  { key: 'observer', role: 'Наблюдатель', label: 'Кабинет наблюдателя', purpose: 'Просмотр доступных туров и результатов соревнования своего сообщества.', sections: [['overview', 'Обзор'], ['tours', 'Доступные туры'], ['results', 'Результаты']] },
  { key: 'supervisor', role: 'Супервайзер', label: 'Кабинет супервайзера', purpose: 'Роль управления пользователями в пределах одного сообщества.', sections: [['overview', 'Обзор'], ['purpose', 'Назначение'], ['capabilities', 'Возможности']] },
];
export const routeDefinitions = [
  { key: 'home', title: 'Главная', cabinetKey: null, sectionKey: null },
  { key: 'login', title: 'Вход и регистрация', cabinetKey: null, sectionKey: null },
  ...cabinetDefinitions.flatMap(cabinet => cabinet.sections.map(([sectionKey, title]) => ({ key: `${cabinet.key}.${sectionKey}`, title, cabinetKey: cabinet.key, sectionKey }))),
];
const legacyRoutes = { profile: 'student.overview', org: 'organizer.overview' };
export function resolvePrototypeRoute(value) {
  let key;
  try { key = decodeURIComponent(String(value || '').replace(/^#\/?/, '')); } catch { return routeDefinitions[0]; }
  key = legacyRoutes[key] || key;
  return routeDefinitions.find(route => route.key === key) || routeDefinitions[0];
}
export function getCabinetRoute(role) {
  const cabinet = cabinetDefinitions.find(cabinet => cabinet.key === role || cabinet.role === role);
  return cabinet ? `${cabinet.key}.overview` : 'home';
}
export function filterPrototypeMenu(menu, query = '') {
  const normalized = String(query).trim().toLocaleLowerCase('ru-RU');
  if (!normalized) return menu;
  const cols = menu.cols.flatMap(column => {
    const roleMatches = [column.title, column.searchText || ''].join(' ').toLocaleLowerCase('ru-RU').includes(normalized);
    const links = roleMatches ? column.links : column.links.filter(link => String(link.t).toLocaleLowerCase('ru-RU').includes(normalized));
    return links.length || (roleMatches && column.target) ? [{ ...column, links }] : [];
  });
  return { ...menu, cols };
}
export function getHeaderItems(query = '') {
  const menu = { cols: cabinetDefinitions.map(cabinet => ({
    key: cabinet.key,
    title: cabinet.role,
    target: getCabinetRoute(cabinet.key),
    searchText: cabinet.label,
    links: cabinet.sections.filter(([key]) => key !== 'overview').map(([key, label]) => ({ key, t: label, target: `${cabinet.key}.${key}` })),
  })) };
  return [
    { key: 'home', label: 'Главная', target: 'home' },
    { key: 'menu', label: 'Меню', menu: filterPrototypeMenu(menu, query) },
  ];
}
export function getBreadcrumbItems(value) {
  const route = resolvePrototypeRoute(value);
  if (route.key === 'home') return [{ label: 'Главная', target: 'home' }];
  const cabinet = cabinetDefinitions.find(cabinet => cabinet.key === route.cabinetKey);
  if (!cabinet) return [{ label: 'Главная', target: 'home' }, { label: route.title, target: route.key }];
  const items = [{ label: 'Главная', target: 'home' }, { label: cabinet.label, target: getCabinetRoute(cabinet.key) }];
  if (route.sectionKey !== 'overview') items.push({ label: route.title, target: route.key });
  return items;
}
export function selectionContextKey(cabinetKey, actorId) { return `${cabinetKey}:${actorId}`; }
export function updatePrototypeSelection(selections, cabinetKey, actorId, patch) {
  const key = selectionContextKey(cabinetKey, actorId);
  return { ...selections, [key]: { ...selections[key], ...patch } };
}
