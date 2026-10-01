import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cabinetDefinitions, routeDefinitions, resolvePrototypeRoute, getCabinetRoute, getHeaderItems, getBreadcrumbItems, updatePrototypeSelection, filterPrototypeMenu } from '../ui_kits/platform/PrototypeNavigation.mjs';
import { createDemoState, reduceDemoState, getCabinetActor, getCabinetObjects, getOrganizerAvailableTours } from '../ui_kits/platform/DemoModel.mjs';

test('should expose exactly the eight cabinet overviews', () => {
  assert.deepEqual(cabinetDefinitions.map(cabinet => getCabinetRoute(cabinet.role)), ['student.overview', 'organizer.overview', 'developer.overview', 'judge.overview', 'administrator.overview', 'participant.overview', 'observer.overview', 'supervisor.overview']);
});
for (const cabinet of cabinetDefinitions) {
  test(`should route every menu target of ${cabinet.key} to its own section`, () => {
    const group = getHeaderItems()[1].menu.cols.find(column => column.key === cabinet.key);
    assert.equal(group.target, `${cabinet.key}.overview`);
    assert.deepEqual(group.links.map(link => resolvePrototypeRoute(link.target).sectionKey), cabinet.sections.filter(([key]) => key !== 'overview').map(([key]) => key));
    assert.ok(group.links.every(link => resolvePrototypeRoute(link.target).cabinetKey === cabinet.key));
  });
  test(`should select the matching demo actor for ${cabinet.key} without changing session roles`, () => {
    const state = createDemoState();
    const actor = getCabinetActor(state, cabinet.role);
    assert.equal(actor.role, cabinet.role);
    assert.equal(state.sessionUserId, null);
    assert.deepEqual(actor.communityIds, ['community1']);
  });
}
for (const [value, expected] of [['', 'home'], ['unknown', 'home'], ['#%E0%A4', 'home'], ['#student.study', 'student.study'], ['#/participant.overview', 'participant.overview'], ['#organizer.results', 'organizer.results'], ['profile', 'student.overview'], ['org', 'organizer.overview'], ['login', 'login']]) {
  test(`should resolve ${JSON.stringify(value)} to ${expected}`, () => assert.equal(resolvePrototypeRoute(value).key, expected));
}
test('should expose home and one menu containing all eight role groups', () => {
  const items = getHeaderItems();
  assert.deepEqual(items.map(item => item.key), ['home', 'menu']);
  assert.equal(items[0].target, 'home');
  assert.equal(items[1].label, 'Меню');
  assert.deepEqual(items[1].menu.cols.map(column => column.title), ['Ученик', 'Организатор', 'Разработчик', 'Судья', 'Администратор', 'Участник', 'Наблюдатель', 'Супервайзер']);
  assert.equal(items[1].menu.cols.flatMap(column => column.links).length, 20);
  assert.deepEqual(items[1].menu.cols.map(column => column.target), ['student.overview', 'organizer.overview', 'developer.overview', 'judge.overview', 'administrator.overview', 'participant.overview', 'observer.overview', 'supervisor.overview']);
  assert.equal(items[1].menu.cols.some(column => column.links.some(link => link.t === 'Обзор')), false);
  assert.equal(new Set(items[1].menu.cols.flatMap(column => [column.target, ...column.links.map(link => link.target)])).size, 28);
});
test('should keep the complete tree for an empty or whitespace query', () => {
  const menu = getHeaderItems()[1].menu;
  assert.equal(filterPrototypeMenu(menu, ''), menu);
  assert.equal(filterPrototypeMenu(menu, '  '), menu);
});
test('should keep all sections of a matching role independent of case', () => {
  const menu = getHeaderItems('  оРгАнИзАтОр ')[1].menu;
  assert.deepEqual(menu.cols.map(column => column.key), ['organizer']);
  assert.equal(menu.cols[0].target, 'organizer.overview');
  assert.deepEqual(menu.cols[0].links.map(link => link.target), ['organizer.competitions', 'organizer.participants', 'organizer.tours', 'organizer.results']);
});
test('should match a cabinet title and preserve its children', () => {
  assert.deepEqual(getHeaderItems('Кабинет судьи')[1].menu.cols[0].links.map(link => link.target), ['judge.solutions', 'judge.result']);
});
test('should retain only matching sections under their role headings', () => {
  const menu = getHeaderItems('результат')[1].menu;
  assert.deepEqual(menu.cols.map(column => column.key), ['organizer', 'judge', 'observer']);
  assert.deepEqual(menu.cols.flatMap(column => column.links).map(link => link.target), ['organizer.results', 'judge.result', 'observer.results']);
});
test('should remove groups without matching children', () => {
  const menu = getHeaderItems('ресурс')[1].menu;
  assert.deepEqual(menu.cols.map(column => column.key), ['developer']);
  assert.deepEqual(menu.cols[0].links.map(link => link.target), ['developer.resources']);
  assert.deepEqual(getHeaderItems('не существует')[1].menu.cols, []);
});
test('should preserve source targets and source tree during filtering', () => {
  const original = getHeaderItems()[1].menu;
  const filtered = filterPrototypeMenu(original, 'решения');
  assert.deepEqual(filtered.cols.flatMap(column => column.links).map(link => link.target), ['student.solutions', 'judge.solutions', 'participant.solutions']);
  assert.equal(original.cols.length, 8);
  assert.equal(original.cols.flatMap(column => column.links).length, 20);
});
test('should preserve a matching role heading even without child sections', () => {
  const menu = { cols: [{ key: 'student', title: 'Ученик', target: 'student.overview', links: [] }] };
  assert.deepEqual(filterPrototypeMenu(menu, 'УЧЕНИК').cols.map(column => column.target), ['student.overview']);
  assert.deepEqual(filterPrototypeMenu(menu, 'Профиль').cols, []);
});
test('should keep a clickable role heading with a matched section and omit overview submenus', () => {
  const menu = getHeaderItems('Профиль')[1].menu;
  assert.equal(menu.cols[0].target, 'student.overview');
  assert.deepEqual(menu.cols[0].links.map(link => link.target), ['student.profile']);
  assert.deepEqual(getHeaderItems('Обзор')[1].menu.cols, []);
});
test('should build breadcrumb targets independently from display captions', () => {
  assert.deepEqual(getBreadcrumbItems('student.solutions').map(item => item.target), ['home', 'student.overview', 'student.solutions']);
  assert.deepEqual(getBreadcrumbItems('home').map(item => item.target), ['home']);
  assert.deepEqual(getBreadcrumbItems('login').map(item => item.target), ['home', 'login']);
});
test('should preserve selected objects independently by cabinet and actor', () => {
  const selected = updatePrototypeSelection({}, 'student', 'student', { classId: 'c2', tourId: 't2', taskId: 'task3' });
  const changed = updatePrototypeSelection(selected, 'organizer', 'organizer', { competitionId: 'competition1', tourId: 't1' });
  const newActor = updatePrototypeSelection(changed, 'organizer', 'user103', { competitionId: 'competition104' });
  assert.deepEqual(newActor['student:student'], { classId: 'c2', tourId: 't2', taskId: 'task3' });
  assert.deepEqual(newActor['organizer:organizer'], { competitionId: 'competition1', tourId: 't1' });
  assert.deepEqual(newActor['organizer:user103'], { competitionId: 'competition104' });
});
test('should keep fixed participant identity and restrict objects to its competition', () => {
  const state = reduceDemoState(createDemoState(), { type: 'login', code: 'PART-101' });
  const actor = getCabinetActor(state, 'Участник');
  const objects = getCabinetObjects(state, actor);
  assert.equal(actor.id, 'p1');
  assert.deepEqual(objects.tours.map(item => item.id), ['t1']);
  assert.deepEqual(objects.tasks.map(item => item.id), ['task1', 'task2']);
  assert.deepEqual(objects.solutions.map(item => item.id), ['s3']);
  assert.equal(getCabinetActor(state, 'Ученик').id, 'student');
  assert.equal(state.sessionUserId, 'p1');
});
test('should restrict observer tours to its assigned scope', () => {
  const state = createDemoState();
  const objects = getCabinetObjects(state, getCabinetActor(state, 'Наблюдатель'));
  assert.deepEqual(objects.competitions.map(item => item.id), ['competition1']);
  assert.deepEqual(objects.tours.map(item => item.id), ['t1']);
  assert.deepEqual(objects.solutions, []);
});
test('should restrict administrator users to the matching community', () => {
  const state = createDemoState();
  state.users.push({ id: 'foreign', role: 'Ученик', communityIds: ['public'] });
  const objects = getCabinetObjects(state, getCabinetActor(state, 'Администратор'));
  assert.equal(objects.users.some(user => user.id === 'foreign'), false);
  assert.equal(objects.users.some(user => user.id === 'student'), true);
});
test('should use a new organizer session without leaking the fixture competition', () => {
  const pending = reduceDemoState(createDemoState(), { type: 'register', alias: 'Владелец', email: 'new-owner@example.com', role: 'Организатор' });
  const state = reduceDemoState(pending, { type: 'confirmRegistration', code: '246810' });
  const actor = getCabinetActor(state, 'Организатор');
  assert.equal(actor.id, 'user103');
  assert.deepEqual(getCabinetObjects(state, actor).competitions, []);
  const created = reduceDemoState(state, { type: 'createCompetition', name: 'Своё' });
  assert.deepEqual(getCabinetObjects(created, actor).competitions.map(item => item.id), ['competition104']);
  assert.equal(created.competitions.at(-1).communityId, 'public');
});
test('should keep a newly registered student free of fixture classes', () => {
  const pending = reduceDemoState(createDemoState(), { type: 'register', alias: 'Новый ученик', email: 'new-student@example.com', role: 'Ученик' });
  const state = reduceDemoState(pending, { type: 'confirmRegistration', code: '246810' });
  const actor = getCabinetActor(state, 'Ученик');
  assert.equal(actor.id, 'user103');
  assert.deepEqual(getCabinetObjects(state, actor).classes, []);
  assert.deepEqual(getCabinetObjects(state, actor).tasks, []);
});

test('should list only tours shared with the new organizer community and reject another tour', () => {
  const pending = reduceDemoState(createDemoState(), { type: 'register', alias: 'Организатор public', email: 'public-owner@example.com', role: 'Организатор' });
  const registered = reduceDemoState(pending, { type: 'confirmRegistration', code: '246810' });
  const actor = getCabinetActor(registered, 'Организатор');
  const created = reduceDemoState(registered, { type: 'createCompetition', name: 'Public demo' });
  assert.deepEqual(getOrganizerAvailableTours(created, actor).map(tour => tour.id), ['t1']);
  const denied = reduceDemoState(created, { type: 'addTour', competitionId: 'competition104', tourId: 't2' });
  assert.equal(denied.message.tone, 'danger');
  assert.deepEqual(denied.competitions.at(-1).tourIds, []);
  const allowed = reduceDemoState(created, { type: 'addTour', competitionId: 'competition104', tourId: 't1' });
  assert.deepEqual(allowed.competitions.at(-1).tourIds, ['t1']);
  assert.deepEqual(allowed.tasks.filter(task => task.id === 'task1' || task.id === 'task2').map(task => task.communityIds), [['community1', 'public'], ['community1', 'public']]);
});
