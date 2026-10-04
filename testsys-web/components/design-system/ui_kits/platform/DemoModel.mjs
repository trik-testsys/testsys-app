export const DEMO_PARTICIPANT_LIMIT = 100;
// Deterministic fixtures and transitions for the reference prototype; no server operations.
export const solutionKindLabels = { VisualLanguage: 'Визуальная программа TRIK Studio', Python: 'Python', JavaScript: 'JavaScript' };
export const solutionStatusLabels = { Queue: 'В очереди', Checking: 'Проверяется', Checked: 'Проверено', Error: 'Ошибка проверки', Timeout: 'Тайм-аут' };
export const taskStateLabels = { New: 'Новая', Uncommitted: 'Есть незафиксированные изменения', Committed: 'Зафиксирована' };
export function createDemoState() {
  return {
    users: [
      { id: 'student', alias: 'Анна', email: 'anna@example.com', role: 'Ученик', accessCode: 'STUDENT-2026', lastLogin: '01.10.2026 10:00' },
      { id: 'organizer', alias: 'Ирина', email: 'irina@example.com', role: 'Организатор', accessCode: 'ORG-2026', lastLogin: '01.10.2026 09:30' },
      { id: 'p1', alias: 'Участник 1', role: 'Участник', accessCode: 'PART-101', lastLogin: '01.10.2026 10:15' },
      { id: 'p2', alias: 'Участник 2', role: 'Участник', accessCode: 'PART-102', lastLogin: '—' },
      { id: 'developer', alias: 'Максим', email: 'max@example.com', role: 'Разработчик', accessCode: 'DEV-2026', lastLogin: '01.10.2026 09:00' },
      { id: 'judge', alias: 'Ольга', role: 'Судья', accessCode: 'JUDGE-2026', lastLogin: '01.10.2026 10:00' },
      { id: 'administrator', alias: 'Денис', role: 'Администратор', accessCode: 'ADMIN-2026', lastLogin: '01.10.2026 08:30' },
      { id: 'observer', alias: 'Наблюдатель', role: 'Наблюдатель', accessCode: 'OBSERVER-2026', lastLogin: '01.10.2026 10:30' },
      { id: 'supervisor', alias: 'Супервайзер', role: 'Супервайзер', accessCode: 'SUPERVISOR-2026', lastLogin: '01.10.2026 09:15' },
    ].map(user => ({ ...user, communityIds: ['community1'] })),
    communities: [{ id: 'community1', name: 'Лаборатория TRIK' }, { id: 'public', name: 'Публичное сообщество' }],
    observerScopes: [{ userId: 'observer', competitionId: 'competition1', tourIds: ['t1'] }],
    resources: [
      { id: 'r1', ownerId: 'developer', taskId: 'task1', name: 'Условие движения по линии', category: 'Условие', fileName: 'task1-statement.txt', modifiedAt: '30.09.2026 16:00', history: [{ modifiedAt: '30.09.2026 16:00', fileName: 'task1-statement.txt', comment: 'Уточнена точка финиша' }] },
      { id: 'r2', ownerId: 'developer', taskId: 'task1', name: 'Полигон линии', category: 'Полигон', fileName: 'task1-polygon-demo.xml', modifiedAt: '30.09.2026 15:00', history: [{ modifiedAt: '30.09.2026 15:00', fileName: 'task1-polygon-demo.xml', comment: 'Демонстрационная модель мира' }] },
      { id: 'r3', ownerId: 'developer', taskId: 'task1', name: 'Авторское решение Python', category: 'Авторское Решение', fileName: 'task1-author-demo.py', modifiedAt: '30.09.2026 17:00', history: [{ modifiedAt: '30.09.2026 17:00', fileName: 'task1-author-demo.py', comment: 'Добавлен демонстрационный вариант' }] },
    ],
    sessionUserId: null, pendingRegistration: null, recovery: null, message: null, nextId: 103,
    classes: [{ id: 'c1', name: 'Робототехника · 8 класс', studentIds: ['student'], tourIds: ['t1', 't2'] }, { id: 'c2', name: 'Клуб TRIK · начинающие', studentIds: ['student'], tourIds: ['t2'] }, { id: 'c3', name: 'Самостоятельная практика', studentIds: ['student'], tourIds: [] }],
    competitions: [{ id: 'competition1', name: 'Осеннее соревнование TRIK', organizerId: 'organizer', communityId: 'community1', participantIds: ['p1', 'p2'], tourIds: ['t1'] }],
    tours: [
      { id: 't1', name: 'Движение и датчики', description: 'Два упражнения на движение робота и работу датчика расстояния.', startsAt: '01.10.2026 10:00', endsAt: '01.10.2026 14:00', durationMinutes: 120, remainingSeconds: 5400, trikVersion: '2026.1', taskIds: ['task1', 'task2'] },
      { id: 't2', name: 'Маршруты', description: 'Планирование маршрута в модели мира TRIK Studio.', startsAt: '02.10.2026 10:00', endsAt: '02.10.2026 14:00', durationMinutes: 90, remainingSeconds: 5400, trikVersion: '2026.1', taskIds: ['task3'] },
    ].map(tour => ({ ...tour, ownerId: 'developer', communityIds: tour.id === 't1' ? ['community1', 'public'] : ['community1'] })),
    tasks: [
      { id: 'task1', name: 'Движение по линии', description: 'Проведите робота по линии до финиша.', authorSolutionKinds: ['VisualLanguage', 'Python'] },
      { id: 'task2', name: 'Датчик расстояния', description: 'Остановите робота перед препятствием.', authorSolutionKinds: ['VisualLanguage', 'JavaScript'] },
      { id: 'task3', name: 'Обход препятствий', description: 'Найдите маршрут до целевой точки.', authorSolutionKinds: ['VisualLanguage'] },
    ].map(task => ({ ...task, ownerId: 'developer', communityIds: task.id === 'task3' ? ['community1'] : ['community1', 'public'], state: 'Committed', trikVersions: ['2026.1'] })),
    solutions: [
      { id: 's1', userId: 'student', taskId: 'task1', fileName: 'line.qrs', kind: 'VisualLanguage', submittedAt: '01.10.2026 10:20', status: 'Checked', score: 0 },
      { id: 's2', userId: 'student', taskId: 'task1', fileName: 'line.py', kind: 'Python', submittedAt: '01.10.2026 10:35', status: 'Checked', score: 72 },
      { id: 's3', userId: 'p1', taskId: 'task1', fileName: 'robot.qrs', kind: 'VisualLanguage', submittedAt: '01.10.2026 10:30', status: 'Checked', score: 85 },
      { id: 's4', userId: 'p2', taskId: 'task2', fileName: 'sensor.js', kind: 'JavaScript', submittedAt: '01.10.2026 10:40', status: 'Timeout', score: null },
    ],
  };
}
const roleActors = { Ученик: 'student', Организатор: 'organizer', Разработчик: 'developer', Судья: 'judge', Администратор: 'administrator', Участник: 'p1', Наблюдатель: 'observer', Супервайзер: 'supervisor' };
const roleKeys = { Ученик: 'student', Организатор: 'organizer', Разработчик: 'developer', Судья: 'judge', Администратор: 'administrator', Участник: 'participant', Наблюдатель: 'observer', Супервайзер: 'supervisor' };
export function getCabinetScreen(role) { return roleKeys[role] ? `${roleKeys[role]}.overview` : null; }
export function getCabinetActor(state, role) {
  const session = state.users.find(user => user.id === state.sessionUserId);
  return session?.role === role ? session : state.users.find(user => user.id === roleActors[role]);
}
export function getOrganizerAvailableTours(state, actor) {
  return actor?.role === 'Организатор' ? state.tours.filter(tour => tour.communityIds.some(id => actor.communityIds.includes(id))) : [];
}
export function getCabinetObjects(state, actor) {
  const communities = state.communities.filter(community => actor.communityIds.includes(community.id));
  const classes = actor.role === 'Ученик' ? state.classes.filter(group => group.studentIds.includes(actor.id)) : [];
  const competitions = actor.role === 'Организатор' ? state.competitions.filter(item => item.organizerId === actor.id) : actor.role === 'Участник' ? state.competitions.filter(item => item.participantIds.includes(actor.id)) : actor.role === 'Наблюдатель' ? state.competitions.filter(item => actor.communityIds.includes(item.communityId) && state.observerScopes.some(scope => scope.userId === actor.id && scope.competitionId === item.id)) : [];
  const tourIds = actor.role === 'Ученик' ? classes.flatMap(group => group.tourIds) : actor.role === 'Наблюдатель' ? state.observerScopes.filter(scope => scope.userId === actor.id && competitions.some(item => item.id === scope.competitionId)).flatMap(scope => scope.tourIds) : competitions.flatMap(item => item.tourIds);
  const tours = state.tours.filter(tour => actor.role === 'Разработчик' ? tour.ownerId === actor.id : tourIds.includes(tour.id));
  const tasks = state.tasks.filter(task => actor.role === 'Разработчик' ? task.ownerId === actor.id || task.communityIds.some(id => actor.communityIds.includes(id)) : tours.some(tour => tour.taskIds.includes(task.id)));
  const resources = actor.role === 'Разработчик' ? state.resources.filter(resource => resource.ownerId === actor.id) : [];
  const solutions = actor.role === 'Судья' ? state.solutions.filter(solution => state.users.some(user => user.id === solution.userId && ['Ученик', 'Участник'].includes(user.role))) : state.solutions.filter(solution => solution.userId === actor.id && tasks.some(task => task.id === solution.taskId));
  const users = actor.role === 'Администратор' ? state.users.filter(user => user.communityIds.some(id => actor.communityIds.includes(id))) : [];
  return { communities, classes, competitions, tours, tasks, resources, solutions, users };
}
export function getAllowedSolutionKinds(task) { return [...task.authorSolutionKinds]; }
export function getBestScore(solutions, userId, taskId) {
  const values = solutions.filter(s => s.userId === userId && s.taskId === taskId && s.status === 'Checked' && Number.isFinite(s.score)).map(s => s.score);
  return values.length ? Math.max(...values) : null;
}
const normalizeEmail = value => String(value || '').trim().toLowerCase();
export function reduceDemoState(state, action) {
  const fail = text => ({ ...state, message: { tone: 'danger', text } });
  const success = (changes, text) => ({ ...state, ...changes, message: { tone: 'success', text } });
  const organizerId = state.users.find(u => u.id === state.sessionUserId && u.role === 'Организатор')?.id || 'organizer';
  switch (action.type) {
    case 'reset': return createDemoState();
    case 'clearMessage': return { ...state, message: null };
    case 'login': {
      const user = state.users.find(u => u.accessCode === String(action.code).trim());
      return user ? success({ sessionUserId: user.id }, `Вход выполнен: ${user.alias}`) : fail('Код-доступа невалиден');
    }
    case 'register': {
      const email = normalizeEmail(action.email);
      if (!action.alias?.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || !['Ученик', 'Организатор'].includes(action.role)) return fail('Укажите псевдоним, корректную почту и роль');
      if (state.users.some(u => normalizeEmail(u.email) === email)) return fail('Почта уже привязана к кабинету');
      return success({ pendingRegistration: { alias: action.alias.trim(), email, role: action.role, confirmationCode: '246810' } }, 'Демонстрационное письмо подготовлено');
    }
    case 'confirmRegistration': {
      const pending = state.pendingRegistration;
      if (!pending || String(action.code).trim() !== pending.confirmationCode) return fail('Код подтверждения указан неверно');
      if (state.users.some(u => normalizeEmail(u.email) === pending.email)) return fail('Почта уже привязана к кабинету');
      const user = { id: `user${state.nextId}`, alias: pending.alias, email: pending.email, role: pending.role, accessCode: `ACCESS-${state.nextId}`, lastLogin: '—', communityIds: ['public'] };
      return success({ users: [...state.users, user], sessionUserId: user.id, nextId: state.nextId + 1, pendingRegistration: null }, `Регистрация завершена. Код-доступа: ${user.accessCode}`);
    }
    case 'requestRecovery': {
      const user = state.users.find(u => u.email && normalizeEmail(u.email) === normalizeEmail(action.email));
      return user ? success({ recovery: { userId: user.id, token: `restore-${state.nextId}`, completed: false } }, 'Демонстрационное письмо со ссылкой подготовлено') : fail('Кабинет с такой почтой не найден');
    }
    case 'restoreAccess': {
      const recovery = state.recovery;
      if (!recovery || recovery.completed || recovery.token !== action.token) return fail('Ссылка восстановления недействительна');
      const code = `RESTORED-${state.nextId}`;
      return success({ users: state.users.map(u => u.id === recovery.userId ? { ...u, accessCode: code } : u), recovery: { ...recovery, completed: true, accessCode: code }, nextId: state.nextId + 1 }, `Новый код-доступа: ${code}. Старый код невалиден`);
    }
    case 'createCompetition': {
      if (!action.name?.trim()) return fail('Введите название соревнования');
      const competition = { id: `competition${state.nextId}`, name: action.name.trim(), organizerId, communityId: state.users.find(user => user.id === organizerId).communityIds[0], participantIds: [], tourIds: [] };
      return success({ competitions: [...state.competitions, competition], nextId: state.nextId + 1 }, 'Соревнование создано');
    }
    case 'addTour': {
      const competition = state.competitions.find(c => c.id === action.competitionId && c.organizerId === organizerId);
      if (!competition || !getOrganizerAvailableTours(state, state.users.find(user => user.id === organizerId)).some(tour => tour.id === action.tourId)) return fail('Соревнование или тур недоступны');
      if (competition.tourIds.includes(action.tourId)) return fail('Тур уже добавлен');
      return success({ competitions: state.competitions.map(c => c.id === competition.id ? { ...c, tourIds: [...c.tourIds, action.tourId] } : c) }, 'Тур добавлен');
    }
    case 'createParticipants': {
      const count = action.count ?? 3;
      if (!Number.isInteger(count) || count < 1 || count > DEMO_PARTICIPANT_LIMIT) return fail('Укажите целое количество от 1 до 100 (предел демонстрации)');
      const competition = state.competitions.find(c => c.id === action.competitionId && c.organizerId === organizerId);
      if (!competition) return fail('Соревнование недоступно');
      const participants = Array.from({ length: count }, (_, i) => ({ id: `p${state.nextId + i}`, alias: `Участник ${state.nextId + i}`, role: 'Участник', accessCode: `PART-${state.nextId + i}`, lastLogin: '—', communityIds: [competition.communityId] }));
      return success({ users: [...state.users, ...participants], competitions: state.competitions.map(c => c.id === competition.id ? { ...c, participantIds: [...c.participantIds, ...participants.map(p => p.id)] } : c), nextId: state.nextId + count }, `Созданы участники: ${count}; каждому выдан код-доступа`);
    }
    case 'submitSolution': {
      const task = state.tasks.find(t => t.id === action.taskId);
      const user = state.users.find(u => u.id === action.userId);
      const allowedTasks = user?.role === 'Ученик' ? state.classes.filter(c => c.studentIds.includes(user.id)).flatMap(c => c.tourIds).flatMap(id => state.tours.find(t => t.id === id)?.taskIds || []) : state.competitions.filter(c => c.participantIds.includes(action.userId)).flatMap(c => c.tourIds).flatMap(id => state.tours.find(t => t.id === id)?.taskIds || []);
      if (!task || !allowedTasks.includes(task.id) || !getAllowedSolutionKinds(task).includes(action.kind) || !action.fileName?.trim()) return fail('Выберите доступную задачу, вид решения и файл');
      const solution = { id: `s${state.nextId}`, userId: action.userId, taskId: task.id, fileName: action.fileName, kind: action.kind, submittedAt: '01.10.2026 11:00', status: 'Queue', score: null };
      return success({ solutions: [...state.solutions, solution], nextId: state.nextId + 1 }, 'Решение поставлено в очередь демонстрационной проверки');
    }
    case 'checkSolution': return { ...state, solutions: state.solutions.map(s => s.id === action.solutionId && s.status === 'Queue' ? { ...s, status: 'Checking' } : s) };
    case 'finishSolution': return { ...state, solutions: state.solutions.map(s => s.id === action.solutionId && ['Queue', 'Checking'].includes(s.status) ? { ...s, status: action.status || 'Checked', score: action.status && action.status !== 'Checked' ? null : action.score ?? 64 } : s) };
    default: return state;
  }
}
export function buildResultsCsv(state, competitionId, tourId) {
  const competition = state.competitions.find(c => c.id === competitionId);
  const tour = state.tours.find(t => t.id === tourId);
  if (!competition || !tour || !competition.tourIds.includes(tourId)) return '';
  const tasks = tour.taskIds.map(id => state.tasks.find(t => t.id === id));
  const escape = value => '"' + String(value ?? '').replaceAll('"', '""') + '"';
  const rows = [['ID', 'Псевдоним', ...tasks.flatMap(t => [`${t.id} ${t.name}: лучший балл`, `${t.id} ${t.name}: решений`])], ...competition.participantIds.map(id => {
    const user = state.users.find(u => u.id === id);
    return [id, user.alias, ...tasks.flatMap(t => [getBestScore(state.solutions, id, t.id) ?? '', state.solutions.filter(s => s.userId === id && s.taskId === t.id).length])];
  })];
  return '\uFEFF' + rows.map(row => row.map(escape).join(';')).join('\r\n');
}
export function downloadDemoFile(name, content, type = 'text/plain;charset=utf-8') {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement('a'); link.href = url; link.download = name; document.body.appendChild(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
